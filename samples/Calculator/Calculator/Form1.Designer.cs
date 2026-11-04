namespace Calculator;

partial class Form1
{
    /// <summary>
    ///  Required designer variable.
    /// </summary>
    private System.ComponentModel.IContainer components = null;
    private TextBox txtDisplay;
    private Button btn7;
    private Button btn8;
    private Button btn9;
    private Button btnDivide;
    private Button btn4;
    private Button btn5;
    private Button btn6;
    private Button btnMultiply;
    private Button btn1;
    private Button btn2;
    private Button btn3;
    private Button btnMinus;
    private Button btn0;
    private Button btnPoint;
    private Button btnEquals;
    private Button btnPlus;

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
        txtDisplay = new TextBox();
        btn7 = new Button();
        btn8 = new Button();
        btn9 = new Button();
        btnDivide = new Button();
        btn4 = new Button();
        btn5 = new Button();
        btn6 = new Button();
        btnMultiply = new Button();
        btn1 = new Button();
        btn2 = new Button();
        btn3 = new Button();
        btnMinus = new Button();
        btn0 = new Button();
        btnPoint = new Button();
        btnEquals = new Button();
        btnPlus = new Button();
        SuspendLayout();
        //
        // txtDisplay
        //
        txtDisplay.Location = new Point(12, 12);
        txtDisplay.Name = "txtDisplay";
        txtDisplay.ReadOnly = true;
        txtDisplay.Size = new Size(236, 23);
        txtDisplay.TabIndex = 0;
        txtDisplay.Text = "0";
        txtDisplay.TextAlign = HorizontalAlignment.Right;
        //
        // btn7
        //
        btn7.Location = new Point(12, 45);
        btn7.Name = "btn7";
        btn7.Size = new Size(55, 40);
        btn7.TabIndex = 1;
        btn7.Text = "7";
        btn7.UseVisualStyleBackColor = true;
        btn7.Click += Digit_Click;
        //
        // btn8
        //
        btn8.Location = new Point(72, 45);
        btn8.Name = "btn8";
        btn8.Size = new Size(55, 40);
        btn8.TabIndex = 2;
        btn8.Text = "8";
        btn8.UseVisualStyleBackColor = true;
        btn8.Click += Digit_Click;
        //
        // btn9
        //
        btn9.Location = new Point(132, 45);
        btn9.Name = "btn9";
        btn9.Size = new Size(55, 40);
        btn9.TabIndex = 3;
        btn9.Text = "9";
        btn9.UseVisualStyleBackColor = true;
        btn9.Click += Digit_Click;
        //
        // btnDivide
        //
        btnDivide.Location = new Point(192, 45);
        btnDivide.Name = "btnDivide";
        btnDivide.Size = new Size(55, 40);
        btnDivide.TabIndex = 4;
        btnDivide.Text = "/";
        btnDivide.UseVisualStyleBackColor = true;
        btnDivide.Click += Operator_Click;
        //
        // btn4
        //
        btn4.Location = new Point(12, 90);
        btn4.Name = "btn4";
        btn4.Size = new Size(55, 40);
        btn4.TabIndex = 5;
        btn4.Text = "4";
        btn4.UseVisualStyleBackColor = true;
        btn4.Click += Digit_Click;
        //
        // btn5
        //
        btn5.Location = new Point(72, 90);
        btn5.Name = "btn5";
        btn5.Size = new Size(55, 40);
        btn5.TabIndex = 6;
        btn5.Text = "5";
        btn5.UseVisualStyleBackColor = true;
        btn5.Click += Digit_Click;
        //
        // btn6
        //
        btn6.Location = new Point(132, 90);
        btn6.Name = "btn6";
        btn6.Size = new Size(55, 40);
        btn6.TabIndex = 7;
        btn6.Text = "6";
        btn6.UseVisualStyleBackColor = true;
        btn6.Click += Digit_Click;
        //
        // btnMultiply
        //
        btnMultiply.Location = new Point(192, 90);
        btnMultiply.Name = "btnMultiply";
        btnMultiply.Size = new Size(55, 40);
        btnMultiply.TabIndex = 8;
        btnMultiply.Text = "*";
        btnMultiply.UseVisualStyleBackColor = true;
        btnMultiply.Click += Operator_Click;
        //
        // btn1
        //
        btn1.Location = new Point(12, 135);
        btn1.Name = "btn1";
        btn1.Size = new Size(55, 40);
        btn1.TabIndex = 9;
        btn1.Text = "1";
        btn1.UseVisualStyleBackColor = true;
        btn1.Click += Digit_Click;
        //
        // btn2
        //
        btn2.Location = new Point(72, 135);
        btn2.Name = "btn2";
        btn2.Size = new Size(55, 40);
        btn2.TabIndex = 10;
        btn2.Text = "2";
        btn2.UseVisualStyleBackColor = true;
        btn2.Click += Digit_Click;
        //
        // btn3
        //
        btn3.Location = new Point(132, 135);
        btn3.Name = "btn3";
        btn3.Size = new Size(55, 40);
        btn3.TabIndex = 11;
        btn3.Text = "3";
        btn3.UseVisualStyleBackColor = true;
        btn3.Click += Digit_Click;
        //
        // btnMinus
        //
        btnMinus.Location = new Point(192, 135);
        btnMinus.Name = "btnMinus";
        btnMinus.Size = new Size(55, 40);
        btnMinus.TabIndex = 12;
        btnMinus.Text = "-";
        btnMinus.UseVisualStyleBackColor = true;
        btnMinus.Click += Operator_Click;
        //
        // btn0
        //
        btn0.Location = new Point(12, 180);
        btn0.Name = "btn0";
        btn0.Size = new Size(55, 40);
        btn0.TabIndex = 13;
        btn0.Text = "0";
        btn0.UseVisualStyleBackColor = true;
        btn0.Click += Digit_Click;
        //
        // btnPoint
        //
        btnPoint.Location = new Point(72, 180);
        btnPoint.Name = "btnPoint";
        btnPoint.Size = new Size(55, 40);
        btnPoint.TabIndex = 14;
        btnPoint.Text = ".";
        btnPoint.UseVisualStyleBackColor = true;
        btnPoint.Click += Digit_Click;
        //
        // btnEquals
        //
        btnEquals.Location = new Point(132, 180);
        btnEquals.Name = "btnEquals";
        btnEquals.Size = new Size(55, 40);
        btnEquals.TabIndex = 15;
        btnEquals.Text = "=";
        btnEquals.UseVisualStyleBackColor = true;
        btnEquals.Click += btnEquals_Click;
        //
        // btnPlus
        //
        btnPlus.Location = new Point(192, 180);
        btnPlus.Name = "btnPlus";
        btnPlus.Size = new Size(55, 40);
        btnPlus.TabIndex = 16;
        btnPlus.Text = "+";
        btnPlus.UseVisualStyleBackColor = true;
        btnPlus.Click += Operator_Click;
        //
        // Form1
        //
        AutoScaleMode = AutoScaleMode.Font;
        ClientSize = new Size(260, 230);
        Controls.Add(btnPlus);
        Controls.Add(btnEquals);
        Controls.Add(btnPoint);
        Controls.Add(btn0);
        Controls.Add(btnMinus);
        Controls.Add(btn3);
        Controls.Add(btn2);
        Controls.Add(btn1);
        Controls.Add(btnMultiply);
        Controls.Add(btn6);
        Controls.Add(btn5);
        Controls.Add(btn4);
        Controls.Add(btnDivide);
        Controls.Add(btn9);
        Controls.Add(btn8);
        Controls.Add(btn7);
        Controls.Add(txtDisplay);
        FormBorderStyle = FormBorderStyle.FixedSingle;
        MaximizeBox = false;
        Name = "Form1";
        Text = "Calculator";
        ResumeLayout(false);
        PerformLayout();
    }

    #endregion
}
