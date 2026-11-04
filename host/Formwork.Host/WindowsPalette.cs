using System;
using System.Collections;
using System.Reflection;

namespace Formwork.Host;

/// <summary>
/// Visual Studio always draws the designed form in the light Windows 10/11 style: SystemColors.Control is
/// #F0F0F0. Off Windows, .NET's built-in system-colour table is the old Windows XP one (#ECE9D8, beige).
/// NetForms carries the Windows 10/11 table (HermeticRendering.WindowsDefaultPalette, used by its golden
/// tests) but keeps the switch internal, so the designer host turns it on here. This only affects the
/// designer process, never the user's running app.
/// </summary>
internal static class WindowsPalette
{
    public static bool Installed { get; private set; }

    /// <summary>
    /// Text is measured with Selawik (Segoe UI's open-source, metric-compatible replacement, bundled in fonts/)
    /// when Segoe UI isn't installed, so the sizes written to Designer.cs match Visual Studio's more closely.
    /// </summary>
    public static bool InstallFonts()
    {
        try
        {
            var dir = System.IO.Path.Combine(AppContext.BaseDirectory, "fonts");
            var regular = System.IO.Path.Combine(dir, "FormworkSans-Regular.ttf");
            var bold = System.IO.Path.Combine(dir, "FormworkSans-Bold.ttf");
            if (!System.IO.File.Exists(regular)) return false;
            var type = typeof(System.Drawing.Graphics).Assembly.GetType("System.Drawing.HermeticRendering", throwOnError: false);
            var property = type?.GetProperty("FallbackFontFiles", BindingFlags.Public | BindingFlags.Static);
            if (property == null) return false;
            property.SetValue(null, System.IO.File.Exists(bold) ? new[] { regular, bold } : new[] { regular });
            return true;
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Formwork: could not set up the Segoe UI stand-in font: {ex.Message}");
            return false;
        }
    }

    public static void Install()
    {
        try
        {
            var type = typeof(System.Drawing.Graphics).Assembly.GetType("System.Drawing.HermeticRendering", throwOnError: false);
            var palette = type?.GetField("WindowsDefaultPalette", BindingFlags.Public | BindingFlags.Static)?.GetValue(null);
            var property = type?.GetProperty("Palette", BindingFlags.Public | BindingFlags.Static);
            if (palette is IDictionary && property != null)
            {
                property.SetValue(null, palette);
                Installed = true;
            }
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Formwork: could not switch on the Windows palette: {ex.Message}");
        }
    }
}
