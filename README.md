# LPIII Keyboard

A Compose implementation of the Light Phone's keyboard. To be used in LightOS, community tools, and/or as an Android system keyboard.

**Note that as of July 1, 2026, public releases of LightOS are not yet using this as the embedded keyboard. Coming soon!**

If you'd like to contribute/file issues, please read [CONTRIBUTING.md](CONTRIBUTING.md). For general questions/comments about the keyboard, please head to our [discussions](https://github.com/orgs/lightphone/discussions/categories/keyboard) page.

### Layouts

Currently, only English/QWERTY is supported. We want to add more languages/layouts as soon as possible. Please reach out if there are any you are particularly excited about!

## Usage

The `app` module wraps the keyboard into an Android IME app, which can be installed on any Android device

The `ui` module is an Android library that contains all the actual keyboard UI code:

Use the [Lp3Keyboard](ui/src/main/java/com/thelightphone/lp3Keyboard/ui/Lp3Keyboard.kt) composable for "embedded" usage (used in LightOS with some auxiliary UI around it)
```kotlin
@Composable
fun Lp3Keyboard(
    layout: Layout,
    options: KeyboardOptions,
    callback: Lp3KeyboardCallback,
    swipeCallback: Lp3KeyboardSwipeCallback<*>?
) 
```

Use the [Lp3KeyboardWrapper](ui/src/main/java/com/thelightphone/lp3Keyboard/ui/Lp3KeyboardWrapper.kt) composable for a self-contained version (includes a dismiss button)
```kotlin
@Composable
fun Lp3KeyboardWrapper(
    layout: Layout,
    keyboardOptions: KeyboardOptions,
    layoutOptions: LayoutOptions,
    callback: Lp3KeyboardCallback,
    swipeCallback: Lp3KeyboardSwipeCallback<*>?
) 
```

Use the [Lp3RawKeyboardView](ui/src/main/java/com/thelightphone/lp3Keyboard/ui/Lp3KeyboardView.kt) view for mixing in with classic Android views in a Java environment
```kotlin
open class Lp3RawKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) 
```

Use the [Lp3KeyboardView](ui/src/main/java/com/thelightphone/lp3Keyboard/ui/Lp3KeyboardView.kt) view for mixing in with classic Android views in Kotlin
```kotlin
class Lp3RawKeyboardView<T>(context: Context, private val viewModel: Lp3KeyboardViewModel<T>) 
```
