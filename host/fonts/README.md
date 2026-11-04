# Formwork Sans (for the designer's text measurements)

Visual Studio measures text with **Segoe UI**, which isn't on Linux and can't be redistributed. Microsoft's
**Selawik** is an open-source replacement made to have Segoe UI's metrics, so AutoSize labels, text box
heights and similar sizes the designer writes into `Form1.Designer.cs` come out close to what Visual Studio
writes. Formwork.Host uses it **only when Segoe UI isn't installed**, and only inside the designer process.

Selawik's widths are close to Segoe UI's, but its line height is shorter (14.4 px at 9pt instead of Segoe UI's
text height 15 / line height 16), which made text boxes 22 px tall instead of Visual Studio's 23. So the host uses
**Formwork Sans**: Selawik's glyphs, unchanged, with vertical metrics set to give Segoe UI's 15/16 px at 9pt
(`make_formwork_sans.py`). The OFL says a modified font must not use the Reserved Font Name, hence the new name.

Measured with the designer (new control, default size), compared with sizes in Visual Studio-written
`Designer.cs` files (NetForms' test fixtures): Button 75×23, TextBox 100×23, ComboBox 121×23,
NumericUpDown 120×23, DateTimePicker 200×23, CheckBox 83×19, GroupBox 200×100 are **identical**; Label "label1"
is 37×15 (VS 38×15), LinkLabel 58×15 (VS 60×15), RadioButton 96×19 (VS 94×19), ListBox 120×96 (VS 120×94).

- Licence: SIL Open Font License 1.1 (`Selawik-OFL.txt`). Copyright 2015 Microsoft Corporation, Reserved Font
  Name "Selawik". The font is unmodified apart from being compiled from Microsoft's sources.
- Source: https://github.com/microsoft/Selawik at commit `89362e84731d1f5777fa078fd4d5ebbd339e4378` (tag 1.01).
- Built with `fontmake==3.10.1`:
  `fontmake -u "Source files/UFO/Selawik-Regular.ufo" -o ttf --output-path Selawik-Regular.ttf` (same for Bold).

| File | SHA-256 |
|---|---|
| Selawik-Regular.ttf | `2eb25847cca6c03238203131f0a6fbf319ab0a5348eedf1bd329fcdca686a077` |
| Selawik-Bold.ttf | `9416518b6bca1d4588ba24e5bf2ce205a6386bbfa49c5f584d312ea624d74209` |
| FormworkSans-Regular.ttf | `6bfb45a64f0a17ca005c6af8ae36e75a058e161e0d3e297b5bc50b9011fed0f1` |
| FormworkSans-Bold.ttf | `bf4738a32a97979e0b8b332dc247a8697f1d929f4bf4ad1820da2bcf2b878831` |

Formwork Sans is made with `python make_formwork_sans.py Selawik-Regular.ttf FormworkSans-Regular.ttf Regular`
(and `... Bold`), using the fontTools that comes with fontmake 3.10.1.
