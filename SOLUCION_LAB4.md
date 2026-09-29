# Laboratorio 4 – Solución paso a paso

App **Sabores del Mundo**: RecyclerView + Fragments + Navigation Component + Acelerómetro, consumiendo **TheMealDB**.
Lenguaje: Java · ViewBinding · Retrofit 2.9.0 + Gson 2.10 · Navigation + Safe Args · `minSdk 34`.

> **Todo está alineado con lo que el profe usa en clase** (repos `2026-2-1TEL05-AppsIot`, Clase5 a Clase7). Ver §12.
>
> Antes de entregar: cambia tu **nombre y código** en `res/values/strings.xml` (`elaborado_por`) y agrega la marca de uso de LLM si aplica (ver §11).

---

## 0. Mapa de la rúbrica → dónde se cumple

| Criterio (pts) | Qué pide | Dónde está |
|---|---|---|
| Interfaces (3) | Todas las pantallas con sus componentes | `activity_main`, `activity_app`, `fragment_*`, `item_*` |
| Fragments y navegación (4) | 3 fragments, navegación fluida, backstack | `nav_graph.xml`, `AppActivity`, `NavHelper` |
| Sensores (3) | Acelerómetro leído, procesado e integrado | `SensorAccListener`, `MealsFragment` |
| RecyclerView + servicio externo (5) | Lista dinámica desde API, buenas prácticas | `CategoryAdapter`, `MealAdapter`, `MealApi`, `ApiClient` |
| Funcionalidad (5) | Todo el flujo funciona | Toda la app (§10 checklist) |

Buenas prácticas aplicadas (para el criterio del RecyclerView): `RecyclerView.Adapter` con ViewHolder + ViewBinding (como Clase5), listener por interfaz (el adapter no navega), red asíncrona con `enqueue` (no bloquea el hilo principal), `cancel()` de llamadas y `binding = null` en `onDestroyView`, validación de respuestas nulas/vacías, textos en `strings.xml`.

---

## 1. Estructura del proyecto

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/lab4/
│   ├── MainActivity.java        ← pantalla principal + validación de internet
│   ├── AppActivity.java         ← NavHost + BottomNavigationView
│   ├── CategoriesFragment.java  ← GET 1
│   ├── MealsFragment.java       ← GET 2A / 2B + acelerómetro (GET 4)
│   ├── RecipeFragment.java      ← GET 3
│   ├── CategoryAdapter.java / MealAdapter.java
│   ├── SensorAccListener.java   ← SensorEventListener + detección de agitación
│   ├── MealApi.java / ApiClient.java   ← Retrofit
│   ├── ImageLoader.java         ← imágenes desde URL (sin Glide)
│   ├── NavHelper.java           ← navegación sin acumular backstack
│   └── Category, CategoriesResponse, MealSummary, MealsResponse,
│       RecipeResponse, RecipeDetail   ← modelos del JSON
└── res/
    ├── layout/   activity_main, activity_app, fragment_*, item_category, item_meal
    ├── menu/bottom_nav_menu.xml
    ├── navigation/nav_graph.xml
    └── values/strings.xml
```

---

## 2. Paso 0 – Configuración (Gradle y permisos)

**`gradle/libs.versions.toml`** – se agregan `navigation-fragment`, `navigation-ui` (2.9.6) y el plugin `navigation-safeargs`.

**`build.gradle` (raíz)** y **`app/build.gradle`** – plugin Safe Args (igual que Clase6):
```groovy
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.navigation.safeargs)   // genera las clases *Directions y *Args
}
android { buildFeatures { viewBinding true } }   // genera ActivityMainBinding, FragmentMealsBinding, ...

dependencies {
    implementation libs.navigation.fragment
    implementation libs.navigation.ui
    // Mismas versiones que Clase5 / Clase7
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.google.code.gson:gson:2.10'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
}
```

**`AndroidManifest.xml`**
```xml
<uses-permission android:name="android.permission.INTERNET" />              <!-- llamar a la API -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />  <!-- saber si hay red -->
```
y se registran `MainActivity` (launcher) y `AppActivity`.

---

## 3. Pantalla principal – `MainActivity` (2.5 pts)

**Layout `activity_main.xml`** (ConstraintLayout), de arriba a abajo:
1. `TextView` con el nombre de la app (`@string/app_name`).
2. `ImageView` con una imagen de gastronomía (vector `ic_restaurant`, funciona sin internet).
3. `Button` **Ingresar**.
4. `TextView` **"Elaborado por: Nombre/Código"** como último elemento (anclado al fondo).

**Validar internet (`MainActivity.java`)**
```java
private boolean hayInternet() {
    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    Network network = cm.getActiveNetwork();                       // red activa (null = sin red)
    if (network == null) return false;
    NetworkCapabilities caps = cm.getNetworkCapabilities(network);
    return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
}
```
**Botón Ingresar:** si hay internet → `startActivity(new Intent(this, AppActivity.class))`; si no → dialog (mismo `MaterialAlertDialogBuilder` de Clase7):
```java
new MaterialAlertDialogBuilder(this)
    .setTitle(R.string.dialog_sin_internet_titulo)
    .setMessage(R.string.dialog_sin_internet_mensaje)
    .setPositiveButton(R.string.dialog_configuracion,
        (d, w) -> startActivity(new Intent(Settings.ACTION_SETTINGS)))   // Intent implícito a Ajustes
    .setNegativeButton(R.string.dialog_cancelar, null)
    .show();
```
No se llama a `startActivity(AppActivity)` en la rama sin internet → **se impide el acceso**.

---

## 4. Diseño y navegación – `AppActivity` (2 pts + navegación)

### 4.1 Layout `activity_app.xml`
Un `LinearLayout` vertical con:
- `FragmentContainerView` → `android:name="androidx.navigation.fragment.NavHostFragment"`, `app:navGraph="@navigation/nav_graph"`, `app:defaultNavHost="true"` (igual que `activity_navigation.xml` de Clase6). Es el "contenedor" donde se cambian los fragments.
- `BottomNavigationView` → `app:menu="@menu/bottom_nav_menu"` (3 botones).

### 4.2 Grafo `nav_graph.xml`
Define 3 destinos (`categoriesFragment` = inicio, `mealsFragment`, `recipeFragment`), 2 **acciones** y 2 **argumentos** (Safe Args):
```xml
<fragment android:id="@+id/categoriesFragment" ...>
    <action android:id="@+id/action_categoriesFragment_to_mealsFragment" app:destination="@id/mealsFragment"/>
</fragment>
<fragment android:id="@+id/mealsFragment" ...>
    <argument android:name="category" app:argType="string" app:nullable="true" android:defaultValue="@null"/>
    <action android:id="@+id/action_mealsFragment_to_recipeFragment" app:destination="@id/recipeFragment"/>
</fragment>
<fragment android:id="@+id/recipeFragment" ...>
    <argument android:name="mealId" app:argType="string" app:nullable="true" android:defaultValue="@null"/>
</fragment>
```
Los argumentos son **nullable con default `@null`**: si llegan `null` el fragment funciona "a mano" (buscador / ID manual); si llegan con valor, carga automáticamente.

### 4.3 Menú `bottom_nav_menu.xml`
Tres `item` con **los mismos ids** que los destinos del grafo (`categoriesFragment`, `mealsFragment`, `recipeFragment`); eso permite navegar con `item.getItemId()`.

### 4.4 BackStack limpio – `NavHelper`
Requisito: pese a navegar mucho, **atrás debe volver al MainActivity**. Se usa `popUpTo` + `popUpToInclusive` (Clase6, `action_fragmentC_to_fragmentA`), pero por código:
```java
NavOptions options = new NavOptions.Builder()
        .setLaunchSingleTop(true)
        .setPopUpTo(nav.getGraph().getStartDestinationId(), true)   // saca todo hasta el inicio (incluido)
        .build();
nav.navigate(destinationId, null, options);   // desde el menú inferior
nav.navigate(directions, options);            // desde un fragment (clase *Directions de Safe Args)
```
Con esto el stack de fragments **nunca crece** (siempre queda 1 fragment). Además `AppActivity` refuerza el requisito:
```java
getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
    @Override public void handleOnBackPressed() { finish(); }   // vuelve a MainActivity
});
```
`MainActivity` sigue viva debajo porque se abrió con `startActivity` sin `finish()`.

### 4.5 `AppActivity.java`
```java
NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHostFragment);
navController = navHost.getNavController();

binding.bottomNav.setOnItemSelectedListener(item -> {
    NavHelper.navigateTo(navController, item.getItemId());   // clic en el menú
    return true;
});
navController.addOnDestinationChangedListener((c, dest, args) -> {  // resalta el botón correcto
    MenuItem item = binding.bottomNav.getMenu().findItem(dest.getId());
    if (item != null) item.setChecked(true);
});
```
El segundo listener es necesario porque al navegar **por código** (tocar una categoría) el menú no se entera solo.

---

## 5. Consumo de la API – capa de red (base de los 4 métodos GET)

**Modelos (Gson)**: los nombres de los campos coinciden con el JSON (`strCategory`, `strMealThumb`, …) y Gson los llena solo (igual que `EmployeeDto` en Clase5).
- `CategoriesResponse { List<Category> categories }`
- `MealsResponse { List<MealSummary> meals }` (`meals` llega `null` si no hay resultados → se valida).
- `RecipeResponse { List<Map<String,String>> meals }`: el detalle trae `strIngredient1..20` y `strMeasure1..20`; se recibe como mapa y `RecipeDetail.from(map)` arma el nombre, categoría, área, instrucciones e **ingredientes** (recorre 1..20 y omite los vacíos).

**`MealApi` (Retrofit)** – un método por cada GET del enunciado:
```java
@GET("categories.php") Call<CategoriesResponse> getCategories();                       // GET 1
@GET("filter.php")     Call<MealsResponse> filterByCategory(@Query("c") String c);     // GET 2A
@GET("filter.php")     Call<MealsResponse> filterByIngredient(@Query("i") String i);   // GET 2B
@GET("lookup.php")     Call<RecipeResponse> lookup(@Query("i") String idMeal);         // GET 3
@GET("random.php")     Call<RecipeResponse> random();                                  // GET 4
@GET                   Call<ResponseBody> downloadImage(@Url String url);              // imágenes
```
**`ApiClient`**: `Retrofit.Builder().baseUrl("https://www.themealdb.com/api/json/v1/1/")` + `GsonConverterFactory`. La key pública `1` va dentro de la base URL.

Igual que en Clase5/7, todas las llamadas usan `call.enqueue(new Callback<...>() {...})`: se ejecutan en segundo plano y devuelven el resultado en el hilo principal, así que se puede actualizar la UI directamente.

---

## 6. Fragment Categories – RecyclerView (4 pts)

**Layout:** `RecyclerView` + `ProgressBar`. **Item (`item_category.xml`):** `MaterialCardView` con `ImageView` (imagen) y `TextView` (nombre).

**Adapter (`CategoryAdapter`)**
- Extiende `RecyclerView.Adapter<CategoryViewHolder>` (mismo esquema que `EmployeeAdapter` de Clase5): `setListaCategorias(lista)` + `notifyDataSetChanged()`, `onCreateViewHolder` infla el binding, `onBindViewHolder` rellena la fila, `getItemCount` = tamaño de la lista.
- `onBindViewHolder` → `tvCategory.setText(strCategory)` y `ImageLoader.load(strCategoryThumb, imageView)`.
- El clic se delega por la interfaz `OnCategoryClickListener`.

**Imágenes sin Glide – `ImageLoader`:** en clase no se usa Glide/Picasso, así que la imagen se descarga con el mismo Retrofit (`@GET @Url` → `ResponseBody`), se decodifica con `BitmapFactory` y se guarda en un `LruCache`. Se usa `imageView.setTag(url)` para que una vista reciclada por el RecyclerView no muestre una imagen ajena.

**Fragment (`CategoriesFragment`)**
1. `onCreateView`: infla el binding.
2. `onViewCreated`: crea el adapter, asigna `LinearLayoutManager` y llama a `getCategories()` (**GET 1**).
3. `onResponse`: `adapter.setListaCategorias(lista)`; en error muestra un `Toast`.
4. Al tocar una categoría (Safe Args, como `FragmentA` de Clase6):
```java
NavHelper.navigateTo(NavHostFragment.findNavController(this),
        CategoriesFragmentDirections.actionCategoriesFragmentToMealsFragment()
                .setCategory(category.getStrCategory()));
```
5. `onDestroyView`: `call.cancel(); binding = null;`

---

## 7. Fragment Meals – RecyclerView (6 pts)

Un solo fragment con **dos modos**, según si recibió el argumento `category`:

```java
categoria = getArguments() != null
        ? MealsFragmentArgs.fromBundle(getArguments()).getCategory() : null;   // Safe Args
```

### 7.1 Primera parte (3 pts) – desde Categories → GET 2A
- Se oculta la barra de búsqueda, se muestra el título "Platos de la categoría: X" y se llama `filterByCategory(categoria)`.

### 7.2 Segunda parte (3 pts) – búsqueda por ingrediente → GET 2B
- Si `categoria == null` se muestra `EditText` (hint = placeholder, ej. `chicken_breast`) + botón **Buscar**.
- Al pulsar: se valida que no esté vacío y se llama `filterByIngredient(ingrediente)`.

### 7.3 Lista de platos (`MealAdapter`, `item_meal.xml`)
Cada fila muestra **nombre** (`strMeal`), **ID** (`idMeal`, visible como "ID: 52959") e **imagen** (`strMealThumb`).
Al tocar un plato se navega a Recipe mandando el id:
```java
NavHelper.navigateTo(nav, MealsFragmentDirections
        .actionMealsFragmentToRecipeFragment().setMealId(meal.getIdMeal()));
```

### 7.4 Casos borde
`meals == null` o vacío → lista vacía + mensaje "No se encontraron resultados"; fallo de red → `Toast`; se cancela la llamada anterior antes de lanzar otra búsqueda.

---

## 8. Fragment Recipe – Vista detalle (3.5 pts)

**Layout:** `EditText` (ID, placeholder) + botón **Buscar** arriba; debajo un `ScrollView` (oculto hasta tener datos) con imagen, nombre, categoría, origen, ingredientes e instrucciones, más un `ProgressBar`.

**Lógica (`RecipeFragment`)**
1. Botón Buscar: valida ID no vacío → `buscarReceta(id)` (**GET 3**, `lookup(id)`).
2. `onResponse` → `RecipeDetail.from(meals.get(0))` y `mostrarReceta(...)`:
   - **Nombre**, **Categoría**, **Área/Origen** (`strArea`), **Instrucciones** (`strInstructions`), **Ingredientes** (lista con "• ingrediente - medida", todos los que existan → cumple "al menos 3").
3. **Carga automática:** en `onViewCreated` se lee el argumento (Safe Args, como `FragmentB` de Clase6):
```java
String idRecibido = getArguments() != null
        ? RecipeFragmentArgs.fromBundle(getArguments()).getMealId() : null;
if (idRecibido != null) { binding.etIdPlato.setText(idRecibido); buscarReceta(idRecibido); }
```
Si se entra por el menú inferior, el argumento es `null` y el usuario escribe el ID (ej. `52959`).

---

## 9. Sensor – Acelerómetro (2 pts)

> Interpretación: "fragmento de búsqueda principal" = el **fragment Meals en modo búsqueda** (el que tiene el campo de ingrediente). Si el profesor se refiere a otro, solo cambia la condición de registro en `onResume`.

Se sigue el patrón de `SensorActivity` / `SensorAccListener` de Clase5: un `SensorManager`, `getDefaultSensor(TYPE_ACCELEROMETER)` (validando `null`), una clase **`SensorAccListener implements SensorEventListener`** y `registerListener` / `unregisterListener` según el ciclo de vida.

### 9.1 Registrar y liberar (solo mientras el fragment está visible) – `MealsFragment`
```java
private final SensorAccListener listener = new SensorAccListener(this::pedirPlatoSorpresa);

@Override public void onResume() {
    super.onResume();
    if (categoria != null) return;                         // solo en modo búsqueda
    if (acelerometro == null) { /* Toast: sin sensor */ return; }
    listener.reiniciar();                                  // evita falso disparo al iniciar
    sensorManager.registerListener(listener, acelerometro, SensorManager.SENSOR_DELAY_NORMAL);
}
@Override public void onPause() { super.onPause(); sensorManager.unregisterListener(listener); }
```

### 9.2 Detectar la agitación (> 4 m/s²) – `SensorAccListener.onSensorChanged`
El acelerómetro incluye la gravedad (~9.8 m/s²), por eso se aplica un **filtro paso-alto** para quedarse solo con el movimiento:
```java
for (int i = 0; i < 3; i++) {
    gravedad[i] = ALPHA * gravedad[i] + (1 - ALPHA) * sensorEvent.values[i];   // estima la gravedad
    lineal[i]   = sensorEvent.values[i] - gravedad[i];                         // movimiento puro
}
double magnitud = Math.sqrt(lineal[0]*lineal[0] + lineal[1]*lineal[1] + lineal[2]*lineal[2]);
if (magnitud > 4f && ahora - ultimaAgitacion > 2000) {   // umbral + antirrebote de 2 s
    ultimaAgitacion = ahora;
    listener.onShake();          // avisa al fragment -> pedirPlatoSorpresa()
}
```

### 9.3 Plato sorpresa → GET 4
`ApiClient.get().random()` → al responder se muestra un `MaterialAlertDialogBuilder` "¡Plato sorpresa!" con nombre, categoría y origen, y el botón **Ver receta**, que navega a Recipe con el `idMeal` (Recipe lo carga solo, como en §8). Así se cubren las dos opciones del enunciado (Dialog **y** redirección a Recipe).

---

## 10. Cómo probar (checklist de funcionalidad)

1. Abrir la app → se ve título, imagen, botón Ingresar y "Elaborado por" al final.
2. Activar **modo avión** → Ingresar → aparece el Dialog; **Configuración** abre Ajustes; la app no entra.
3. Con internet → Ingresar → carga la lista de **categorías** con imagen y nombre.
4. Tocar una categoría (ej. Seafood) → pasa a **Meals** con sus platos y el botón inferior "Meals" resaltado.
5. Tocar un plato → pasa a **Recipe** con la receta cargada sola (nombre, categoría, origen, ingredientes, instrucciones).
6. Menú inferior → **Meals** → aparece el buscador → escribir `chicken_breast` → Buscar → lista de platos.
7. En esa pantalla **agitar el teléfono** → Dialog de plato sorpresa → Ver receta.
8. Menú inferior → **Recipe** → escribir `52959` → Buscar.
9. Navegar entre los 3 botones varias veces y presionar **Atrás** → vuelve a MainActivity.
10. En **Categories** agitar el teléfono → no debe pasar nada (sensor inactivo).

Para probar el acelerómetro en el emulador: *Extended controls (⋯) → Virtual sensors → Move*, o "Shake" en versiones recientes.

---

## 11. Entrega

- Repo GitHub llamado **`IOT_LAB4_<tu código>`**; se revisa solo el **último commit** antes de la hora límite.
- **Video ≤ 5 min** mostrando los puntos del checklist (sin video se califica sobre 10).
- **Nombre y código** reales en `strings.xml` → `elaborado_por`.
- **Uso de LLM:** si se usó, indícalo en el código con el formato dado en clase (no lo conozco, por eso no lo inventé).

---

## 12. Qué usa el profe en clase (repos `2026-2-1TEL05-AppsIot`) y cómo se aplicó

Revisados: Clase5, Clase5-WebService, Clase6, Clase7, Clase7-WebService.

| Tema | Lo que usa el profe | Dónde se aplicó aquí |
|---|---|---|
| HTTP / JSON | `Retrofit 2.9.0` + `gson 2.10` + `converter-gson 2.9.0`; interfaz con `@GET`; `Call<...>.enqueue(new Callback<>(){onResponse/onFailure})`; clases DTO con getters | `MealApi`, `ApiClient`, modelos `*Response` |
| RecyclerView | `RecyclerView.Adapter` + `ViewHolder` con ViewBinding, `setLista...()`, `LinearLayoutManager` | `CategoryAdapter`, `MealAdapter` |
| Sensores | `SensorManager`, `getDefaultSensor(TYPE_ACCELEROMETER)`, clase aparte `SensorAccListener implements SensorEventListener`, `registerListener` en `onResume` y `unregisterListener` al salir | `SensorAccListener`, `MealsFragment` |
| Fragments / Navigation | `NavHostFragment` en `FragmentContainerView`, `nav_graph.xml` con `<action>` y `<argument>`, **Safe Args** (`FragmentADirections`, `FragmentBArgs`), `popUpTo` / `popUpToInclusive` | `nav_graph.xml`, `NavHelper`, fragments |
| Dialogs | `MaterialAlertDialogBuilder(...).setTitle().setMessage().setPositiveButton().show()` | `MainActivity` (sin internet), `MealsFragment` (plato sorpresa) |
| Otros | `Toast`, `Log.d("msg-test-...")`, `Intent` con extras, ViewBinding en todo, permisos `INTERNET` y `ACCESS_NETWORK_STATE`, `registerForActivityResult` | Toasts en errores; permisos en el manifest |

**No aparece en los repos (por eso se evitó o se reemplazó):**
- **Glide / Picasso / Volley** → las imágenes se cargan con Retrofit + `BitmapFactory` (`ImageLoader`).
- **`ListAdapter` / `DiffUtil`** → adapter clásico con `notifyDataSetChanged()`.
- `androidx.appcompat.app.AlertDialog` → se usa `MaterialAlertDialogBuilder`.

**Obligatorio aunque no salga en los repos** (lo pide el enunciado): `BottomNavigationView` (parte de Material, ya incluida) y `ConnectivityManager` para validar internet (el permiso `ACCESS_NETWORK_STATE` sí lo declara el profe en Clase5).

> Clase8 en adelante todavía no está publicada; si el lab de hoy incluye algo más reciente, hay que revisarlo.
