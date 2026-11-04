namespace TimerPicture;

partial class Form1
{
    /// <summary>
    ///  Required designer variable.
    /// </summary>
    private System.ComponentModel.IContainer components = null;
    private PictureBox pictureBox1;
    private Label lblTime;
    private System.Windows.Forms.Timer timer1;

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
        pictureBox1 = new PictureBox();
        lblTime = new Label();
        timer1 = new System.Windows.Forms.Timer(components);
        ((System.ComponentModel.ISupportInitialize)pictureBox1).BeginInit();
        SuspendLayout();
        //
        // pictureBox1
        //
        pictureBox1.BackColor = Color.White;
        pictureBox1.BorderStyle = BorderStyle.FixedSingle;
        pictureBox1.Location = new Point(12, 12);
        pictureBox1.Name = "pictureBox1";
        pictureBox1.Size = new Size(200, 150);
        pictureBox1.TabIndex = 0;
        pictureBox1.TabStop = false;
        //
        // lblTime
        //
        lblTime.AutoSize = true;
        lblTime.Font = new Font("Segoe UI", 14.25F);
        lblTime.Location = new Point(12, 175);
        lblTime.Name = "lblTime";
        lblTime.Size = new Size(77, 24);
        lblTime.TabIndex = 1;
        lblTime.Text = "00:00:00";
        //
        // timer1
        //
        timer1.Enabled = true;
        timer1.Interval = 1000;
        timer1.Tick += timer1_Tick;
        //
        // Form1
        //
        AutoScaleMode = AutoScaleMode.Font;
        ClientSize = new Size(224, 215);
        Controls.Add(lblTime);
        Controls.Add(pictureBox1);
        Name = "Form1";
        Text = "Timer and Picture";
        Load += Form1_Load;
        ((System.ComponentModel.ISupportInitialize)pictureBox1).EndInit();
        ResumeLayout(false);
        PerformLayout();
    }

    #endregion
}
