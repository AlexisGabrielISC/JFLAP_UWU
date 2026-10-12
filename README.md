# JFLAP

```text
La modificación echa, fue por el odio que se le tomo a la version de JFLAP :3
```

Versión modificada de JFLAP. Esta versión se centra en el editor y el simulador de **Máquinas de Turing**, pero varias mejoras aplican a todos los autómatas.

## Requisitos

- **JDK / JRE 21 o superior** (`java -version` debe mostrar 21+).

## Ejecutar

```
java -jar JFLAP_UWU_v4.jar
```

## Ejecutar en caso de haber descargado la versión del relase.
```
java -jar JFLAP_v4.jar
```

Tema claro en lugar del oscuro: `java -Djflap.dark=false -jar JFLAP_UWU_v4.jar`
Alternar el tema en caliente: **Ctrl + Shift + D**.

---

## Modificaciones echas

### 1. Borrar con Supr (selección múltiple)

Selecciona varios nodos y/o transiciones (con el rectángulo de selección o con clics) y pulsa **Supr** (en Mac, Retroceso). Se borran todos de una vez, incluidas las transiciones conectadas a los nodos borrados.

- Se puede deshacer con **Ctrl + Z** (rehacer: Ctrl + Shift + Z o Ctrl + Y).
- No actúa mientras escribes en una caja de texto o editas una tabla.

### 2. Botón **Auto ▶** en el simulador

En la barra inferior de la simulación (junto a Step, Reset, Freeze…):

| Control | Función |
|---|---|
| **Auto ▶** | Avanza paso a paso automáticamente. Mientras corre, el botón cambia a **Pause ⏸**. |
| Campo `ms` | Milisegundos entre pasos (50 – 10000, por defecto 500). Se puede cambiar mientras corre. |
| Etiqueta de resultado | Muestra **Accepted** o **Rejected** al terminar. |

Se detiene solo cuando alguna configuración **acepta**, o cuando ya no queda ninguna configuración válida (**rechaza**). **Reset** también lo detiene. Si la máquina entra en un bucle infinito, no se detiene sola: usa **Pause**.

### 3. Notas legibles y con formato

El texto de las notas ahora es oscuro fijo sobre su fondo, por lo que se lee igual en tema claro y oscuro.

**Clic derecho sobre una nota** abre el menú:

- **Font / size…** — familia, tamaño, negrita, cursiva.
- **Text color…** y **Background color…**
- **Box size…** — ancho y alto de la caja (el texto se ajusta en varias líneas).
- **Auto box size** — la caja vuelve a ajustarse sola al texto.
- **Reset style**

(Para crear una nota: clic derecho en un espacio vacío del lienzo → **Add Note**.)

### 4. Color de nodos

**Clic derecho sobre un nodo → Color… / Reset color.** El color se aplica al nodo y a todos los nodos que estén seleccionados. El nombre del nodo cambia a blanco o negro automáticamente según el contraste.

### 5. Alfabetos en las transiciones (`\d`, `\w`, `\W`…)

En el campo **Read** de una transición de Turing (y en las etiquetas de autómatas finitos) se pueden usar clases de caracteres:

| Expresión | Coincide con |
|---|---|
| `\d` | dígitos `0-9` |
| `\D` | cualquier carácter que no sea dígito |
| `\w` | letras y dígitos `A-Za-z0-9` |
| `\W` | caracteres especiales (ni letra/dígito ASCII, ni espacio): `$ # ! - _ …` |
| `\s` / `\S` | espacio en blanco / no espacio en blanco |

Pueden usarse solos o dentro de corchetes, combinados con rangos y con negación:

```
[\d]        dígitos
[\w]        = [A-Za-z0-9]
[\W]        caracteres especiales
[\dA-F]     dígitos o A–F (hexadecimal)
[^\d]       cualquier cosa menos dígitos
[\d\W]      dígitos o caracteres especiales
[a-z], [ABDC], [0875]. etc. rangos
```

Notas:

- `\D`, `\W` y `\S` **no** coinciden con el blanco de la cinta (□). Para detectar el final de la cadena usa □ de forma explícita.
- `\d` ya no significa la letra "d" literal. Para una barra invertida o un corchete literal escribe `\\` o `\[`.
- En el campo **Write** se sigue usando un solo símbolo, o `~` para conservar el que ya estaba en la cinta.
- Funciona con una o varias cintas.
- No está conectado a los autómatas de pila (PDA).

**Ejemplo:** máquina que acepta cadenas formadas solo por dígitos:

```
q0 --[\d] ; ~ , R--> q0
q0 --□    ; □ , S--> q1      (q1 es final)
```

Acepta `12345` y rechaza `12a45`.

### 6. Guardado y deshacer

- Los **colores de nodos** y el **formato de las notas** se guardan en el `.jff` (como elementos XML adicionales) y se recuperan al abrir el archivo. Los archivos antiguos siguen abriéndose normalmente.
- **Deshacer** conserva colores y formato de las notas. Además se corrigió un fallo heredado: en máquinas de Turing, deshacer hacía desaparecer las notas.

---

## Atajos

| Atajo | Acción |
|---|---|
| Supr / Retroceso | Borrar selección (nodos y transiciones) |
| Ctrl + Z | Deshacer |
| Ctrl + Shift + Z / Ctrl + Y | Rehacer |
| 1 … 9 | Elegir la herramienta de la barra del editor (por posición) |
| Ctrl + Shift + D | Cambiar tema claro/oscuro |

---

## Compilar desde las fuentes (Windows)

1. Instala un **JDK 21+** (por ejemplo Temurin) y comprueba con `javac -version`.
2. Descomprime `fuentes_v4.zip` en una carpeta.
3. Copia tu JAR original a esa carpeta con el nombre **`JFLAP_UWU.jar`** (necesita la extensión `.jar`).
4. Ejecuta `build.bat`. Genera **`JFLAP_v4.jar`**.

El script compila solo las clases modificadas contra el JAR original y las inserta en una copia del mismo. El resto del programa queda intacto.

### Clases modificadas

| Archivo | Cambio |
|---|---|
| `plus/JFLAPPlus` | Atajo Supr / Retroceso |
| `plus/LabelMatcher` | Motor de clases y alfabetos |
| `gui/sim/ControlPanel`, `ConfigurationController` | Botón Auto y su lógica de aceptar/rechazar |
| `automata/Note` | Notas legibles, menú de formato, tamaño de caja |
| `automata/State`, `gui/viewer/StateDrawer`, `gui/editor/ArrowTool` | Color de nodos |
| `gui/editor/AutomatonSizeSlider` | El zoom respeta la fuente elegida en las notas |
| `automata/Automaton`, `automata/turing/TuringMachine` | Copiar color y formato al deshacer; conservar notas en Turing |
| `automata/turing/TMTransition`, `TMSimulator` | Clases de caracteres en el campo Read |
| `file/xml/AutomatonTransducer` | Guardar/leer color de nodos y formato de notas |

> Estas clases se obtuvieron decompilando el JAR (no había código fuente), y algunas se corrigieron a mano para que compilaran. Si notas un comportamiento raro al arrastrar nodos o abrir archivos antiguos, repórtalo.

---

## Limitaciones conocidas

- Los iconos de la barra de herramientas se ven oscuros sobre fondo oscuro en el tema oscuro.
- Aún no se puede cambiar la fuente global de iconos y textos.
- En máquinas de Turing, la negación `!a` no funcionó en las pruebas, igual que en el JAR original (comportamiento heredado).
- **Edit Block** (menú del nodo) abre el editor de la subrutina del bloque, que está vacío si el bloque es nuevo; no es un editor de propiedades del nodo (para eso usa **Color…** y **Set Name**).

