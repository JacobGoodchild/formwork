# Third-party notices

Penguin Forms (MIT, see `LICENSE`) ships the following third-party software. In the plugin, the full licence and notice
files are in `licenses/` (`licenses/NetForms`, `licenses/packages/<package>`, `licenses/fonts`).

| Component | Used for | Licence | Copyright |
|---|---|---|---|
| NetForms 0.1.5 (with the changes in `upstream-patches/`) | Windows Forms on Linux, the designer host and design-surface page, the project converter, form templates | MIT | NetForms contributors (its own notices: `licenses/NetForms/THIRD-PARTY-NOTICES.md`) |
| Avalonia 12.1.1 (Avalonia, .Base, .Controls, .Desktop, .FreeDesktop, .HarfBuzz, .Skia, .X11 and the other Avalonia assemblies in `host/`) | NetForms' platform layer | MIT | © 2013-2026 The AvaloniaUI Project |
| SkiaSharp 3.119.4 (with Skia) | Drawing | MIT; Skia: BSD-3-Clause | © Microsoft Corporation; Skia © Google LLC and others (`licenses/packages/SkiaSharp.NativeAssets.Linux`) |
| HarfBuzzSharp 8.3.1.3 (with HarfBuzz) | Text shaping | MIT; HarfBuzz: "Old MIT" | © Microsoft Corporation; HarfBuzz © its authors (`licenses/packages/HarfBuzzSharp.NativeAssets.Linux`) |
| Microsoft.CodeAnalysis (Roslyn) 4.9.2 | Reading and writing `InitializeComponent()` | MIT | © Microsoft Corporation (`licenses/packages/Microsoft.CodeAnalysis.Common`) |
| System.Configuration.ConfigurationManager, System.Diagnostics.EventLog, System.Formats.Nrbf, System.Reflection.MetadataLoadContext, System.Resources.Extensions, System.Security.Cryptography.ProtectedData 10.0.0 | .NET libraries used by NetForms | MIT | © .NET Foundation and Contributors (`licenses/packages/System.Configuration.ConfigurationManager`) |
| MicroCom.Runtime 0.11.6 | Used by Avalonia | MIT | © 2021 Nikita Tsukanov |
| Tmds.DBus.Protocol 0.94.1 | Used by Avalonia on Linux | MIT | © Tom Deseyn |
| Windows Form and User Control templates (the text Add → Windows Form / User Control writes) | New forms exactly as Visual Studio makes them | MIT | © .NET Foundation and Contributors (Microsoft's .NET SDK templates, dotnet/winforms) |
| Selawik (fonts `Selawik-*.ttf`, and `FormworkSans-*.ttf` made from it with different vertical metrics) | Measuring text like Segoe UI in the designer | SIL Open Font License 1.1 | © 2015 Microsoft Corporation, Reserved Font Name "Selawik" (`licenses/fonts/Selawik-OFL.txt`) |

The MIT licence, as it applies to each MIT component above with its copyright line:

```
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

Penguin Forms' own icons and Toolbox images are its own drawings. Nothing is copied from Visual Studio. Where
Penguin Forms matches Visual Studio's designer behaviour (for example snapline distances), it follows Microsoft's
open-source dotnet/winforms (MIT) as a description of that behaviour.
