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
