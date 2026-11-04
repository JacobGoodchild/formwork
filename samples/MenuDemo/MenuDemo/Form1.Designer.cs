namespace MenuDemo;

partial class Form1
{
    /// <summary>
    ///  Required designer variable.
    /// </summary>
    private System.ComponentModel.IContainer components = null;
    private MenuStrip menuStrip1;
    private StatusStrip statusStrip1;
    private ContextMenuStrip contextMenuStrip1;
    private OpenFileDialog openFileDialog1;
    private TextBox textBox1;
    private ToolStripMenuItem fileToolStripMenuItem;
    private ToolStripMenuItem helpToolStripMenuItem;
    private ToolStripMenuItem openToolStripMenuItem;
    private ToolStripSeparator toolStripSeparator1;
    private ToolStripMenuItem exitToolStripMenuItem;
    private ToolStripMenuItem aboutToolStripMenuItem;
    private ToolStripStatusLabel toolStripStatusLabel1;
    private ToolStripMenuItem cutToolStripMenuItem;
    private ToolStripMenuItem copyToolStripMenuItem;
    private ToolStripMenuItem pasteToolStripMenuItem;

    /// <summary>
    ///  Clean up any resources being used.
    /// </summary>
    /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
    protected override void Dispose(bool disposing)
    {
        if (disposing && (components != null))
        {
            components.Dispose();
        }
        base.Dispose(disposing);
    }

    #region Windows Form Designer generated code

    /// <summary>
    ///  Required method for Designer support - do not modify
    ///  the contents of this method with the code editor.
    /// </summary>
    private void InitializeComponent()
    {
        components = new System.ComponentModel.Container();
        menuStrip1 = new MenuStrip();
        statusStrip1 = new StatusStrip();
        contextMenuStrip1 = new ContextMenuStrip(components);
        openFileDialog1 = new OpenFileDialog();
        textBox1 = new TextBox();
        fileToolStripMenuItem = new ToolStripMenuItem();
        helpToolStripMenuItem = new ToolStripMenuItem();
        openToolStripMenuItem = new ToolStripMenuItem();
        toolStripSeparator1 = new ToolStripSeparator();
        exitToolStripMenuItem = new ToolStripMenuItem();
        aboutToolStripMenuItem = new ToolStripMenuItem();
        toolStripStatusLabel1 = new ToolStripStatusLabel();
        cutToolStripMenuItem = new ToolStripMenuItem();
        copyToolStripMenuItem = new ToolStripMenuItem();
        pasteToolStripMenuItem = new ToolStripMenuItem();
        menuStrip1.SuspendLayout();
        statusStrip1.SuspendLayout();
        contextMenuStrip1.SuspendLayout();
        SuspendLayout();
        //
        // menuStrip1
        //
        menuStrip1.Items.AddRange(new ToolStripItem[] { fileToolStripMenuItem, helpToolStripMenuItem });
        menuStrip1.Location = new Point(0, 0);
        menuStrip1.Name = "menuStrip1";
        menuStrip1.Size = new Size(384, 24);
        menuStrip1.TabIndex = 0;
        //
        // statusStrip1
        //
        statusStrip1.Items.AddRange(new ToolStripItem[] { toolStripStatusLabel1 });
        statusStrip1.Location = new Point(0, 249);
        statusStrip1.Name = "statusStrip1";
        statusStrip1.Size = new Size(384, 22);
        statusStrip1.TabIndex = 1;
        //
        // contextMenuStrip1
        //
        contextMenuStrip1.Items.AddRange(new ToolStripItem[] { cutToolStripMenuItem, copyToolStripMenuItem, pasteToolStripMenuItem });
        contextMenuStrip1.Name = "contextMenuStrip1";
        contextMenuStrip1.Size = new Size(78, 70);
        //
        // textBox1
        //
        textBox1.ContextMenuStrip = contextMenuStrip1;
        textBox1.Location = new Point(12, 36);
        textBox1.Multiline = true;
        textBox1.Name = "textBox1";
        textBox1.Size = new Size(360, 200);
        textBox1.TabIndex = 2;
        //
        // fileToolStripMenuItem
        //
        fileToolStripMenuItem.DropDownItems.AddRange(new ToolStripItem[] { openToolStripMenuItem, toolStripSeparator1, exitToolStripMenuItem });
        fileToolStripMenuItem.Name = "fileToolStripMenuItem";
        fileToolStripMenuItem.Size = new Size(36, 20);
        fileToolStripMenuItem.Text = "&File";
        //
        // helpToolStripMenuItem
        //
        helpToolStripMenuItem.DropDownItems.AddRange(new ToolStripItem[] { aboutToolStripMenuItem });
        helpToolStripMenuItem.Name = "helpToolStripMenuItem";
        helpToolStripMenuItem.Size = new Size(41, 20);
        helpToolStripMenuItem.Text = "&Help";
        //
        // openToolStripMenuItem
        //
        openToolStripMenuItem.Name = "openToolStripMenuItem";
        openToolStripMenuItem.Size = new Size(85, 22);
        openToolStripMenuItem.Text = "&Open...";
        openToolStripMenuItem.Click += openToolStripMenuItem_Click;
        //
        // toolStripSeparator1
        //
        toolStripSeparator1.Name = "toolStripSeparator1";
        toolStripSeparator1.Size = new Size(85, 6);
        //
        // exitToolStripMenuItem
        //
        exitToolStripMenuItem.Name = "exitToolStripMenuItem";
        exitToolStripMenuItem.Size = new Size(85, 22);
        exitToolStripMenuItem.Text = "E&xit";
        exitToolStripMenuItem.Click += exitToolStripMenuItem_Click;
        //
        // aboutToolStripMenuItem
        //
        aboutToolStripMenuItem.Name = "aboutToolStripMenuItem";
        aboutToolStripMenuItem.Size = new Size(80, 22);
        aboutToolStripMenuItem.Text = "&About";
        //
        // toolStripStatusLabel1
        //
        toolStripStatusLabel1.Name = "toolStripStatusLabel1";
        toolStripStatusLabel1.Size = new Size(40, 17);
        toolStripStatusLabel1.Text = "Ready";
        //
        // cutToolStripMenuItem
        //
        cutToolStripMenuItem.Name = "cutToolStripMenuItem";
        cutToolStripMenuItem.Size = new Size(76, 22);
        cutToolStripMenuItem.Text = "Cu&t";
        //
        // copyToolStripMenuItem
        //
        copyToolStripMenuItem.Name = "copyToolStripMenuItem";
        copyToolStripMenuItem.Size = new Size(76, 22);
        copyToolStripMenuItem.Text = "&Copy";
        //
        // pasteToolStripMenuItem
        //
        pasteToolStripMenuItem.Name = "pasteToolStripMenuItem";
        pasteToolStripMenuItem.Size = new Size(76, 22);
        pasteToolStripMenuItem.Text = "&Paste";
        //
        // Form1
        //
        AutoScaleMode = AutoScaleMode.Font;
        ClientSize = new Size(384, 271);
        Controls.Add(textBox1);
        Controls.Add(statusStrip1);
        Controls.Add(menuStrip1);
        MainMenuStrip = menuStrip1;
        Name = "Form1";
        Text = "Menu Demo";
        menuStrip1.ResumeLayout(false);
        menuStrip1.PerformLayout();
        statusStrip1.ResumeLayout(false);
        statusStrip1.PerformLayout();
        contextMenuStrip1.ResumeLayout(false);
        contextMenuStrip1.PerformLayout();
        ResumeLayout(false);
        PerformLayout();
    }

    #endregion
}
