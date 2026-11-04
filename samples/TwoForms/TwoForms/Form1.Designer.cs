namespace TwoForms;

partial class Form1
{
    /// <summary>
    ///  Required designer variable.
    /// </summary>
    private System.ComponentModel.IContainer components = null;
    private Button btnOpen;

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
        btnOpen = new Button();
        SuspendLayout();
        //
        // btnOpen
        //
        btnOpen.Location = new Point(20, 20);
        btnOpen.Name = "btnOpen";
        btnOpen.Size = new Size(120, 30);
        btnOpen.TabIndex = 0;
        btnOpen.Text = "Open Form2";
        btnOpen.UseVisualStyleBackColor = true;
        btnOpen.Click += btnOpen_Click;
        //
        // Form1
        //
        AutoScaleMode = AutoScaleMode.Font;
        ClientSize = new Size(800, 450);
        Controls.Add(btnOpen);
        Name = "Form1";
        Text = "Form1";
        ResumeLayout(false);
    }

    #endregion
}
