namespace TabsDemo;

partial class Form1
{
    /// <summary>
    ///  Required designer variable.
    /// </summary>
    private System.ComponentModel.IContainer components = null;
    private TabControl tabControl1;
    private TabPage tabPage1;
    private TabPage tabPage2;
    private GroupBox groupBox1;
    private Panel panel1;
    private SplitContainer splitContainer1;
    private CheckBox checkBox1;
    private RadioButton radioButton1;
    private Button button1;
    private ListBox listBox1;
    private TextBox textBox1;
    private TableLayoutPanel tableLayoutPanel1;
    private FlowLayoutPanel flowLayoutPanel1;
    private Label label1;
    private Button button2;
    private Button button3;

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
        tabControl1 = new TabControl();
        tabPage1 = new TabPage();
        tabPage2 = new TabPage();
        groupBox1 = new GroupBox();
        panel1 = new Panel();
        splitContainer1 = new SplitContainer();
        checkBox1 = new CheckBox();
        radioButton1 = new RadioButton();
        button1 = new Button();
        listBox1 = new ListBox();
        textBox1 = new TextBox();
        tableLayoutPanel1 = new TableLayoutPanel();
        flowLayoutPanel1 = new FlowLayoutPanel();
        label1 = new Label();
        button2 = new Button();
        button3 = new Button();
        tabControl1.SuspendLayout();
        tabPage1.SuspendLayout();
        tabPage2.SuspendLayout();
        groupBox1.SuspendLayout();
        panel1.SuspendLayout();
        ((System.ComponentModel.ISupportInitialize)splitContainer1).BeginInit();
        splitContainer1.Panel1.SuspendLayout();
        splitContainer1.Panel2.SuspendLayout();
        splitContainer1.SuspendLayout();
        tableLayoutPanel1.SuspendLayout();
        flowLayoutPanel1.SuspendLayout();
        SuspendLayout();
        //
        // tabControl1
        //
        tabControl1.Controls.Add(tabPage1);
        tabControl1.Controls.Add(tabPage2);
        tabControl1.Location = new Point(12, 12);
        tabControl1.Name = "tabControl1";
        tabControl1.SelectedIndex = 0;
        tabControl1.Size = new Size(560, 330);
        tabControl1.TabIndex = 0;
        //
        // tabPage1
        //
        tabPage1.Controls.Add(splitContainer1);
        tabPage1.Controls.Add(panel1);
        tabPage1.Controls.Add(groupBox1);
        tabPage1.Location = new Point(4, 24);
        tabPage1.Name = "tabPage1";
        tabPage1.Padding = new Padding(3);
        tabPage1.Size = new Size(552, 302);
        tabPage1.TabIndex = 0;
        tabPage1.Text = "Boxes";
        tabPage1.UseVisualStyleBackColor = true;
        //
        // tabPage2
        //
        tabPage2.Controls.Add(flowLayoutPanel1);
        tabPage2.Controls.Add(tableLayoutPanel1);
        tabPage2.Location = new Point(4, 24);
        tabPage2.Name = "tabPage2";
        tabPage2.Padding = new Padding(3);
        tabPage2.Size = new Size(552, 302);
        tabPage2.TabIndex = 1;
        tabPage2.Text = "Layouts";
        tabPage2.UseVisualStyleBackColor = true;
        //
        // groupBox1
        //
        groupBox1.Controls.Add(radioButton1);
        groupBox1.Controls.Add(checkBox1);
        groupBox1.Location = new Point(6, 6);
        groupBox1.Name = "groupBox1";
        groupBox1.Size = new Size(250, 120);
        groupBox1.TabIndex = 0;
        groupBox1.TabStop = false;
        groupBox1.Text = "Options";
        //
        // panel1
        //
        panel1.BorderStyle = BorderStyle.FixedSingle;
        panel1.Controls.Add(button1);
        panel1.Location = new Point(270, 6);
        panel1.Name = "panel1";
        panel1.Size = new Size(270, 120);
        panel1.TabIndex = 1;
        //
        // splitContainer1
        //
        splitContainer1.Location = new Point(6, 135);
        splitContainer1.Name = "splitContainer1";
        //
        // splitContainer1.Panel1
        //
        splitContainer1.Panel1.Controls.Add(listBox1);
        //
        // splitContainer1.Panel2
        //
        splitContainer1.Panel2.Controls.Add(textBox1);
        splitContainer1.Size = new Size(534, 155);
        splitContainer1.SplitterDistance = 178;
        splitContainer1.TabIndex = 2;
        //
        // checkBox1
        //
        checkBox1.AutoSize = true;
        checkBox1.Location = new Point(12, 24);
        checkBox1.Name = "checkBox1";
        checkBox1.Size = new Size(83, 19);
        checkBox1.TabIndex = 0;
        checkBox1.Text = "checkBox1";
        checkBox1.UseVisualStyleBackColor = true;
        //
        // radioButton1
        //
        radioButton1.AutoSize = true;
        radioButton1.Location = new Point(12, 50);
        radioButton1.Name = "radioButton1";
        radioButton1.Size = new Size(96, 19);
        radioButton1.TabIndex = 1;
        radioButton1.Text = "radioButton1";
        radioButton1.UseVisualStyleBackColor = true;
        //
        // button1
        //
        button1.Location = new Point(12, 12);
        button1.Name = "button1";
        button1.Size = new Size(75, 23);
        button1.TabIndex = 0;
        button1.Text = "button1";
        button1.UseVisualStyleBackColor = true;
        //
        // listBox1
        //
        listBox1.Location = new Point(6, 6);
        listBox1.Name = "listBox1";
        listBox1.Size = new Size(120, 120);
        listBox1.TabIndex = 0;
        //
        // textBox1
        //
        textBox1.Location = new Point(6, 6);
        textBox1.Name = "textBox1";
        textBox1.Size = new Size(200, 23);
        textBox1.TabIndex = 0;
        //
        // tableLayoutPanel1
        //
        tableLayoutPanel1.ColumnCount = 2;
        tableLayoutPanel1.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50F));
        tableLayoutPanel1.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50F));
        tableLayoutPanel1.Controls.Add(label1, 0, 0);
        tableLayoutPanel1.Location = new Point(6, 6);
        tableLayoutPanel1.Name = "tableLayoutPanel1";
        tableLayoutPanel1.RowCount = 2;
        tableLayoutPanel1.RowStyles.Add(new RowStyle(SizeType.Percent, 50F));
        tableLayoutPanel1.RowStyles.Add(new RowStyle(SizeType.Percent, 50F));
        tableLayoutPanel1.Size = new Size(260, 150);
        tableLayoutPanel1.TabIndex = 0;
        //
        // flowLayoutPanel1
        //
        flowLayoutPanel1.Controls.Add(button2);
        flowLayoutPanel1.Controls.Add(button3);
        flowLayoutPanel1.Location = new Point(280, 6);
        flowLayoutPanel1.Name = "flowLayoutPanel1";
        flowLayoutPanel1.Size = new Size(260, 150);
        flowLayoutPanel1.TabIndex = 1;
        //
        // label1
        //
        label1.AutoSize = true;
        label1.Location = new Point(3, 0);
        label1.Name = "label1";
        label1.Size = new Size(37, 15);
        label1.TabIndex = 0;
        label1.Text = "label1";
        //
        // button2
        //
        button2.Location = new Point(3, 3);
        button2.Name = "button2";
        button2.Size = new Size(75, 23);
        button2.TabIndex = 0;
        button2.Text = "button2";
        button2.UseVisualStyleBackColor = true;
        //
        // button3
        //
        button3.Location = new Point(84, 3);
        button3.Name = "button3";
        button3.Size = new Size(75, 23);
        button3.TabIndex = 1;
        button3.Text = "button3";
        button3.UseVisualStyleBackColor = true;
        //
        // Form1
        //
        AutoScaleMode = AutoScaleMode.Font;
        ClientSize = new Size(584, 354);
        Controls.Add(tabControl1);
        Name = "Form1";
        Text = "Tabs and Containers";
        tabControl1.ResumeLayout(false);
        tabPage1.ResumeLayout(false);
        tabPage2.ResumeLayout(false);
        groupBox1.ResumeLayout(false);
        groupBox1.PerformLayout();
        panel1.ResumeLayout(false);
        splitContainer1.Panel1.ResumeLayout(false);
        splitContainer1.Panel2.ResumeLayout(false);
        splitContainer1.Panel2.PerformLayout();
        ((System.ComponentModel.ISupportInitialize)splitContainer1).EndInit();
        splitContainer1.ResumeLayout(false);
        tableLayoutPanel1.ResumeLayout(false);
        tableLayoutPanel1.PerformLayout();
        flowLayoutPanel1.ResumeLayout(false);
        ResumeLayout(false);
    }

    #endregion
}
