# Testing Penguin Forms — a guide for Jacob

Version 2 (end of the first night). Hi Jacob! This guide walks you through trying Penguin Forms on your Linux laptop, step by
step. You don't need to know anything technical: just copy each command exactly, one at a time, and
check you see what the guide says you should see.

**Important, honestly:** I (Claude) couldn't test inside Rider itself, because Rider wants you to log in
with a JetBrains account and I'm not allowed to. I tested everything in IntelliJ IDEA instead, which is
the same program underneath, and the designer worked there. The things only *you* can check are marked
**🔎 Rider check** below. If any of them goes wrong, that's useful to know — see part 5.

---

## 1. Install .NET 10

Penguin Forms' designer, and your apps on Linux, need the free **.NET 10 SDK** from Microsoft.
(If you've already got it, skip to the check at the end of this part.)

Open the **Terminal** (Menu → Terminal). Copy and paste each command below, press Enter, and wait
for it to finish before the next one.

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

3. Tell the terminal where .NET lives (these two lines are added to a settings file, so you only do this once):

   ```bash
   echo 'export DOTNET_ROOT=$HOME/.dotnet' >> ~/.bashrc
   ```

   ```bash
   echo 'export PATH=$PATH:$HOME/.dotnet' >> ~/.bashrc
   ```

4. Make sure a few small Linux libraries NetForms uses are there (Mint usually has them already; this
   asks for your password):

   ```bash
   sudo apt install -y libice6 libsm6 libfontconfig1 libicu70
   ```

5. **Close the terminal and open a new one**, then check it worked:

   ```bash
   dotnet --list-sdks
   ```

   ✅ You should see a line starting with `10.0.` (for example `10.0.403 [/home/jacob/.dotnet/sdk]`).
   If it says "command not found", the new terminal didn't pick up step 3 — try logging out and back in.

6. Tell Rider where .NET is: in Rider open **File → Settings → Build, Execution, Deployment →
   Toolset and Build**, and set **.NET CLI executable path** to `/home/jacob/.dotnet/dotnet`
   (use your own user name if it isn't `jacob`). Click **OK**.

## 2. Install the Penguin Forms plugin in Rider

> **Already have the old "Formwork" plugin?** Remove it first, because Penguin Forms is its new name and Rider sees
> them as two different plugins: **File → Settings → Plugins → Installed**, find **Formwork**, click it, then the
> small arrow next to **Disable** → **Uninstall**, and restart Rider. Your projects and settings files aren't touched.

Penguin Forms works with **Rider 2026.1 or newer** (2026.2 too). To see your version: **Help → About**.


1. Get `penguin-forms-0.1.0.zip`. **Don't unzip it.** Two ways:
   - **Easiest:** on GitHub, open the `formwork` repository → **Actions** → the newest green **CI** run → scroll
     to **Artifacts** → click **penguin-forms-plugin**. That downloads a small zip *containing* `penguin-forms-0.1.0.zip`:
     unzip that outer one once (right-click → Extract Here) to get `penguin-forms-0.1.0.zip`.
   - **Or build it yourself** (needs Java 21 and .NET 10): in a terminal in the repository folder, run
     `./gradlew buildPlugin`; the zip appears in `build/distributions/`.
2. In Rider: **File → Settings → Plugins**.
3. Click the **⚙** (gear) at the top, then **Install Plugin from Disk…**
4. Pick `penguin-forms-0.1.0.zip` and click **OK**, then **Restart IDE**.

✅ After the restart, **Tools** menu has **Make This Project Work on Linux Too…** at the top.

## 3. Try the "definition of done" story with a sample

The `samples` folder of the Penguin Forms repository has Visual Studio projects like the ones you make at
college. Start with `samples/VsTemplate.Net8` (a brand-new "Windows Forms App" from Visual Studio).

1. In Rider: **File → Open**, pick `samples/VsTemplate.Net8/WinFormsApp1/WinFormsApp1.csproj`.
   If Rider asks whether to trust the project, choose **Trust**.
2. A note pops up in the bottom right: **"This is a Windows Forms project"**. Click
   **Make it work on Linux too…**.
   ✅ A window shows the project file now (left) and after (right), with a short explanation. Click
   **Make It Work on Linux**.
   ✅ A note says "Done". A copy of the old file is kept as `WinFormsApp1.csproj.penguinforms.bak`.
   🔎 **Rider check:** Rider should reload the project by itself. If the Solution Explorer looks odd,
   right-click the project → **Reload Project**.
3. In the Solution Explorer, **double-click `Form1.cs`**.
   ✅ The form opens in the designer: a tab called **`Form1.cs [Design]`**, a light grey form on a dark
   background, the **Toolbox** on the left (with *Common Controls* open, *Pointer* first) and
   **Properties** on the right. Your code is not open yet - just like Visual Studio.
   🔎 **Rider check:** does `Form1.Designer.cs` appear *under* `Form1.cs` in the Solution Explorer (with a ▸
   arrow), like in Visual Studio?
   🔎 **Rider check (new):** design and code are now **two separate tabs**, like Visual Studio. Press **F7**:
   a second tab, `Form1.cs`, opens with your code. **Shift+F7** goes back to `Form1.cs [Design]`. If the tabs
   act strangely in Rider, you can go back to the old layout (one tab with Design/Text at the bottom) in
   **Settings → Tools → Penguin Forms** by unticking "Design and code in separate tabs" - and please tell me.
4. In the Toolbox, click **Button** (it turns blue), then click somewhere on the form.
   ✅ `button1` appears where you clicked, with small square handles around it. The Toolbox goes back to
   **Pointer**, and the tab title now ends in `*` (meaning "not saved yet").
   You can also draw a rectangle on the form after clicking Button, to choose its size, or drag Button
   straight from the Toolbox onto the form.
5. **Double-click `button1`** on the form.
   ✅ Rider opens your code in a second tab, `Form1.cs`, with a new method `button1_Click` and the cursor
   blinking inside it — just like Visual Studio.
6. Type this inside the method:

   ```csharp
   button1.Text = "Clicked!";
   ```

7. Press **Ctrl+S** to save, then press **Run** (the green ▶ at the top).
   🔎 **Rider check:** a window opens with your button. Click it: it should say "Clicked!".
   (The window's background may look a little beige rather than grey on Linux — that's a known
   NetForms difference, see the README.)
8. Go back to the designer with **Shift+F7**, and press **Ctrl+Z**: the last change is undone.
   **F7** takes you back to the code.

## 4. Make one of your own college projects work on Linux

### Which kind of project is it?

Open the project's `.csproj` file in a text editor (right-click → Open With → Text Editor) and look
at the **first line**:

- **`<Project Sdk="Microsoft.NET.Sdk">`** → the *new kind* (.NET 6/7/8/9/10). ✅ Penguin Forms can convert this.
- **`<?xml version="1.0" encoding="utf-8"?>`** followed by **`<Project ToolsVersion="...`** → the *old
  kind* (.NET Framework 4.x). ⚠️ Penguin Forms can convert this too, but it's riskier: the college half becomes
  a new-style "net48" project. You'll see a warning. **Always check it at college before relying on it**
  (see "Check it at college" below), and keep the `.penguinforms.bak` file.

### Convert it (new kind)

1. Copy the whole project folder from college (USB stick, OneDrive, GitHub…) onto your laptop.
2. Open the `.csproj` in Rider and click **Make it work on Linux too…** when the note appears (or use
   **Tools → Make This Project Work on Linux Too…**).
3. Double-click your form, change something small, save, press Run.

### Check it at college (important!)

Penguin Forms' change is designed so Visual Studio on Windows sees exactly what it had before. But please
check it before you rely on it:

1. Take the project back to college and open it in Visual Studio.
2. ✅ It should open with no errors. Double-click `Form1.cs`: the designer should show your form.
3. Press **Start** (F5): your app should run as usual.
4. (This is also checked automatically on a Windows computer in the cloud every time Penguin Forms changes, with all the samples — but your own projects are worth checking once.)
5. If anything is wrong: your original project file is saved next to it as `<name>.csproj.penguinforms.bak`.
   Delete the `.csproj`, rename the `.bak` file back to `.csproj`, and tell me what happened (part 5).

## How to check the add-a-control fix

You found that adding a control (drag, click-then-click, or drawing a rectangle) did nothing in Rider 2026.1.
The new plugin sends the designer's messages to Rider one at a time and writes every step of an add into Rider's
log, and if an add fails you now get a red note in the bottom-right corner saying why.

1. Get the newest plugin zip (newest green **CI** run → **Artifacts** → **penguin-forms-plugin**, as in part 2) and
   install it the same way (Rider asks to restart: say yes).
2. Open `samples/VsTemplate.Net8` again, double-click `Form1.cs`, and try all three ways: drag **Button** onto the
   form; click **Button** then click the form; click **Button** then draw a rectangle on the form.
3. ✅ Each time a new button (`button1`, `button2`, …) should appear.
4. Whatever happens, copy the log lines about adding: open the **Terminal** and paste this:

   ```bash
   grep -i "penguin forms add" ~/.cache/JetBrains/Rider*/log/idea.log | tail -40
   ```

   *It prints the last 40 lines that mention "Penguin Forms add". If it works, you'll see lines ending in "done
   (System.Windows.Forms.Button)". If not, the lines say where it stopped.* Paste them into an issue (part 5),
   together with any red note Rider showed.

## 5. If something goes wrong

1. In Rider: **Help → Show Log in Files**. A folder opens with a file called `idea.log`.
2. Open `idea.log` in a text editor, scroll to the bottom, and copy the last 100 or so lines (or search
   for the word `penguin` and copy the lines around it).
3. Make a new "issue" at <https://github.com/jacobgoodchild/formwork/issues>: say what you clicked, what
   you expected, what happened instead, and paste the log lines. A screenshot helps a lot.

## Rider-only checks (things I couldn't test here)

Please tick these off when you try Penguin Forms in Rider:

- [ ] Installing the plugin zip works, and Rider restarts without errors.
- [ ] Double-clicking `Form1.cs` in Rider's **Solution Explorer** opens the designer.
- [ ] `Form1.Designer.cs` is nested under `Form1.cs` in the Solution Explorer.
- [ ] After "Make it work on Linux too", Rider reloads the project and **Build** succeeds.
- [ ] **Run** opens your app's window; **Debug** stops at a breakpoint in `button1_Click`.
- [ ] Only **one** designer opens. (Rider has its own Windows-only designer; if you see two Design tabs, or
      an error about the designer needing Windows, tell me — it's the thing I couldn't check here.)
- [ ] **F7** and **Shift+F7** work (they're also in the right-click menu as View Code / View Designer).
- [ ] Rider's light theme: **File → Settings → Appearance → Theme → Light**. The designer's panels turn
      light too (the form itself always stays light, like Visual Studio).
