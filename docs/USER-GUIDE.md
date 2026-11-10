# Penguin Forms user guide

Penguin Forms gives JetBrains Rider on Linux a Windows Forms designer that looks and works like Visual Studio's.
This guide takes you from nothing to a working form, step by step. You don't need to know anything technical: copy
each command exactly, one at a time, and check you see what the guide says you should see.

---

## 1. Install .NET 10

Penguin Forms' designer, and your apps on Linux, need Microsoft's free **.NET 10 SDK**.
(If you already have it, skip to the check in step 5.)

Open a **Terminal**. Paste each command below, press Enter, and wait for it to finish before the next one.

1. Download Microsoft's installer script:

   ```bash
   wget https://dot.net/v1/dotnet-install.sh -O ~/dotnet-install.sh
   ```

   *This saves a small file called `dotnet-install.sh` in your home folder.*

2. Run it to install .NET 10 (it goes into a hidden folder called `.dotnet` in your home folder):

   ```bash
   bash ~/dotnet-install.sh --channel 10.0
   ```

   *It prints a few lines and ends with "Installation finished successfully."*

3. Tell the terminal where .NET lives (these lines go into a settings file, so you only do this once):

   ```bash
   echo 'export DOTNET_ROOT=$HOME/.dotnet' >> ~/.bashrc
   ```

   ```bash
   echo 'export PATH=$PATH:$HOME/.dotnet' >> ~/.bashrc
   ```

4. Make sure a few small Linux libraries that NetForms uses are there (most desktops have them already; this asks
   for your password). On Ubuntu 22.04 and Linux Mint 21:

   ```bash
   sudo apt install -y libice6 libsm6 libfontconfig1 libicu70
   ```

   *On newer versions, `libicu70` has a newer number (for example `libicu74` on Ubuntu 24.04).*

5. **Close the terminal and open a new one**, then check it worked:

   ```bash
   dotnet --list-runtimes
   ```

   ✅ You should see a line starting with `Microsoft.NETCore.App 10.0.`.
   If it says "command not found", the new terminal didn't pick up step 3: log out and back in.

6. Tell Rider where .NET is: **File → Settings → Build, Execution, Deployment → Toolset and Build**, and set
   **.NET CLI executable path** to `/home/<your user name>/.dotnet/dotnet`. Click **OK**.

**More than one .NET on your computer?** That's fine: Penguin Forms looks in every usual place and uses the first
one that has .NET 10. If it still can't find it, it lists where it looked; you can then set the path yourself in
**File → Settings → Tools → Penguin Forms → .NET (dotnet) program**.

## 2. Install the plugin

Penguin Forms works with **Rider 2026.1 or newer**. To see your version: **Help → About**.

1. In Rider: **File → Settings → Plugins** (or **Plugins** on the welcome screen).
2. Search the **Marketplace** tab for **Penguin Forms** and click **Install**.
   *Have the plugin as a file instead (`penguin-forms-<version>.zip`)? Click the **⚙** at the top, then
   **Install Plugin from Disk…**, and pick the zip. **Don't unzip it.***
3. Restart Rider when it asks.

✅ After the restart, the **Tools** menu has **Make This Project Work on Linux Too…**.

## 3. Your first form

The `samples` folder of the [Penguin Forms repository](https://github.com/JacobGoodchild/penguin-forms) has Visual
Studio projects to try. Start with `samples/VsTemplate.Net8`, a brand-new "Windows Forms App" from Visual Studio.

1. **File → Open**, and pick `samples/VsTemplate.Net8/WinFormsApp1/WinFormsApp1.csproj`.
   If Rider asks whether to trust the project, choose **Trust**.
2. A note pops up in the bottom-right corner: **"This is a Windows Forms project"**. Click
   **Make it work on Linux too…**.
   ✅ A window shows the project file now (left) and after (right), with a short explanation. Click
   **Make It Work on Linux**. A copy of the old file is kept as `WinFormsApp1.csproj.penguinforms.bak`.
3. In the Solution Explorer, **double-click `Form1.cs`**.
   ✅ The form opens in the designer, in a tab called **`Form1.cs [Design]`**, with the **Toolbox** on the left
   and **Properties** on the right, as in Visual Studio.
4. In the Toolbox, click **Button**, then click somewhere on the form.
   ✅ `button1` appears where you clicked. You can also drag a rectangle to choose its size, or drag **Button**
   straight from the Toolbox onto the form.
5. **Double-click `button1`.**
   ✅ Your code opens in a second tab, `Form1.cs`, with a new method `button1_Click` and the cursor inside it.
6. Type this inside the method, then save with **Ctrl+S**:

   ```csharp
   button1.Text = "Clicked!";
   ```

7. Press **Run** (the green ▶ at the top). Your window opens; click the button and it says "Clicked!".
8. **Shift+F7** goes back to the designer, **F7** to the code, and **Ctrl+Z** undoes the last change from either.

Toolbox, Properties and Document Outline only appear in projects where you've opened a form in the designer.

## 4. Use one of your own Windows Forms projects

### Which kind of project is it?

Open the project's `.csproj` file in a text editor and look at the **first line**:

- **`<Project Sdk="Microsoft.NET.Sdk">`**: the *new kind* (.NET 6 to 10). ✅ Penguin Forms converts this.
- **`<?xml version="1.0" encoding="utf-8"?>`** followed by **`<Project ToolsVersion="...`**: the *old kind*
  (.NET Framework 4.x). ⚠️ Penguin Forms converts this too, but it's riskier: the Windows half becomes a new-style
  "net48" project. You'll see a warning. **Check it in Visual Studio on Windows before relying on it**, and keep
  the `.penguinforms.bak` file.

### Convert it

1. Copy the project folder onto your Linux computer.
2. Open the `.csproj` in Rider and click **Make it work on Linux too…** when the note appears (or use
   **Tools → Make This Project Work on Linux Too…**).
3. Double-click your form, change something small, save, and press Run.

Your code files are never changed. Only the project file is, so that it picks the real Windows Forms on Windows and
NetForms on Linux. The original is kept next to it as `<name>.csproj.penguinforms.bak`.

### Check it on Windows

The change is designed so Visual Studio on Windows sees exactly what it had before, but it's worth checking once:

1. Open the project in Visual Studio on Windows.
2. ✅ It opens with no errors, and double-clicking `Form1.cs` shows your form in Visual Studio's designer.
3. Press **Start** (F5): your app runs as usual.
4. If anything is wrong: delete the `.csproj`, rename the `.penguinforms.bak` file back to `.csproj`, and report
   it (part 6).

## 5. What's new

**In this version** (each line is one thing to try):

1. On a scaled screen (150–200 %), the form in the designer is sharp, not blurry.
2. With several .NET versions installed, Penguin Forms uses the one that has .NET 10, and says where it looked if
   there's none.
3. **Tab** / **Shift+Tab** on the form move the selection between controls, instead of leaving the designer.
4. Right-click a control → **Properties** opens the Properties window (even if it was closed) on that control.
5. Select two controls with **Ctrl+click**: Properties is blank wherever their values differ, and so is the
   selection box at the top.
6. Messages about project files show tags such as `<UseWindowsForms>` instead of hiding them.
7. The "Open a form in the designer…" hints in the Toolbox, Properties and Document Outline wrap neatly.
8. Toolbox, Properties and Document Outline no longer appear in projects without forms (such as a console app).

**Known issue:** in your *running* app on a scaled screen, a ComboBox's first drop-down can fill the screen. This is
in NetForms itself; a fix is ready for NetForms (`upstream-patches/0009-…`), and your apps get it once NetForms
includes it.

**Earlier:** two tabs like Visual Studio (`Form1.cs [Design]` and `Form1.cs`); light grey running apps on Linux;
pink text-baseline snaplines; Lock Controls and the Locked property (kept in `Form1.resx`, as Visual Studio does);
dragging menu items between menus; sub-menus via Type Here; StatusStrip / ToolStrip item drop-down; collection
editors (Items, TabPages, Columns, Nodes); smart tags; DisplayMember / ValueMember drop-downs. The full list is in
the plugin's change notes.

## 6. Reporting a problem

1. In Rider: **Help → Show Log in Files**. A folder opens with a file called `idea.log`.
2. Open `idea.log` in a text editor and copy the lines that mention `Penguin Forms` (search for it), plus the last
   50 or so lines.
3. Open an issue at <https://github.com/JacobGoodchild/penguin-forms/issues>: say what you did, what you expected
   and what happened instead, and paste the log lines. A screenshot helps a lot. Check the lines first and remove
   anything you'd rather not share (for example your user name in file paths).

If adding a control ever does nothing, the log lines starting with `Penguin Forms add:` show where it stopped.
