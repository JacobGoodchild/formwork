# Penguin Forms

Hello! This is a (Visual Studio-style) WinForms designer, built for Rider on Linux.



I wanted to build this project so I could use WinForms, in my favourite IDE (Rider) on Linux.

And so, this plugin is designed to supply everything Visual Studio’s WinForms has to offer for this situation. And before you ask - yes, it has `Form.cs` AND Designer.

## How it works

You’ll need **.NET 10**, and **Rider 2026.1 or 2026.2**. If you want to run this on Linux, there’s a button in **Tools → Make This Project Work on Linux Too**. Or you’ll probably see it in the bottom-right as a popup.

**Extra info:** It’s built on [NetForms](https://github.com/Go-Forms/.NetForms) (MIT licence), which makes WinForms code run on Linux.

## Install

To get Penguin Forms up and running, follow my guide:

1. Open a blank, brand-new solution or use the Rider welcome screen. Go to **File → Settings → Plugins**, and search JetBrains Marketplace to install Penguin Forms (alternatively, if you have it downloaded, go to the settings cog at the top right and go **Install Plugin From Disk…** and install the downloaded plugin. **Don’t extract the plugin if it’s a ZIP file**).
2. To get samples, go to this GitHub page: <https://github.com/JacobGoodchild/formwork>. Click the green **Code** button, and click the bottom button which says **Download ZIP**… and again, extract the file.
3. At this point, you may have to restart your IDE. Rider will probably tell you if so.
4. Now to open Penguin Forms. Go to **File → Open → Downloads → formwork-main** (whatever your extracted file is called) **→ samples → VsTemplate.Net8 → WinFormsApp1 → WinFormsApp1.csproj** and double-click.
5. *(for Linux)* If you’re on Linux, go to **Tools → Make This Project Work on Linux Too…**
6. You should be greeted with a blank screen. Double-click `Form1.cs`. You should have a Toolbox on the left, Properties on the right, and your form in the middle.
   - If it’s not working, check you’re not still in the blank solution and you selected the right sample.
   - If it says you don’t have .NET 10 and you think you do, then check your .NET path. Go to **File → Settings → Tools → Penguin Forms** (from the dropdown), find the **.NET path** box (probably the second-to-last box) and paste your full .NET path. It’ll probably be something like `/home/username/.dotnet/dotnet`.
7. Done! Start building something great.

## Credits and licence

- **NetForms** © NetForms contributors, MIT licence — the Windows Forms implementation, the designer host and the design-surface page. Its `LICENSE` and `THIRD-PARTY-NOTICES.md` ship inside the plugin (`penguinforms/licenses/NetForms`). Penguin Forms' small changes to it are in `upstream-patches/`, each explained.
- **Selawik** © Microsoft, SIL Open Font License 1.1 — the basis of Formwork Sans (`host/fonts/`).
- **Penguin Forms** itself: MIT licence ([LICENSE](LICENSE)). Toolbox icons are Penguin Forms' own drawings; nothing is copied from Visual Studio.
- "Rider" and "IntelliJ IDEA" are JetBrains trademarks; "Visual Studio" and "Windows Forms" are Microsoft's; "Linux" is a trademark of Linus Torvalds. They're named only to say what Penguin Forms works with. Penguin Forms is an independent project, not made or endorsed by any of them, and uses none of their logos.
