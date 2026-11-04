namespace LoginForm;

partial class Form1
{
    /// <summary>
    ///  Required designer variable.
    /// </summary>
    private System.ComponentModel.IContainer components = null;
    private Label lblUser;
    private TextBox txtUser;
    private Label lblPassword;
    private TextBox txtPassword;
    private Button btnOK;
    private Button btnCancel;

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
        lblUser = new Label();
        txtUser = new TextBox();
        lblPassword = new Label();
        txtPassword = new TextBox();
        btnOK = new Button();
        btnCancel = new Button();
        SuspendLayout();
        //
        // lblUser
        //
        lblUser.AutoSize = true;
        lblUser.Location = new Point(12, 15);
        lblUser.Name = "lblUser";
        lblUser.Size = new Size(70, 14);
        lblUser.TabIndex = 0;
        lblUser.Text = "Username:";
        //
        // txtUser
        //
        txtUser.Location = new Point(90, 12);
        txtUser.Name = "txtUser";
        txtUser.Size = new Size(180, 21);
        txtUser.TabIndex = 1;
        //
        // lblPassword
        //
        lblPassword.AutoSize = true;
        lblPassword.Location = new Point(12, 48);
        lblPassword.Name = "lblPassword";
        lblPassword.Size = new Size(65, 14);
        lblPassword.TabIndex = 2;
        lblPassword.Text = "Password:";
        //
        // txtPassword
        //
        txtPassword.Location = new Point(90, 45);
        txtPassword.Name = "txtPassword";
        txtPassword.PasswordChar = '*';
        txtPassword.Size = new Size(180, 21);
        txtPassword.TabIndex = 3;
        //
        // btnOK
        //
        btnOK.Location = new Point(114, 85);
        btnOK.Name = "btnOK";
        btnOK.Size = new Size(75, 23);
        btnOK.TabIndex = 4;
        btnOK.Text = "OK";
        btnOK.UseVisualStyleBackColor = true;
        btnOK.Click += btnOK_Click;
        //
        // btnCancel
        //
        btnCancel.DialogResult = DialogResult.Cancel;
        btnCancel.Location = new Point(195, 85);
        btnCancel.Name = "btnCancel";
        btnCancel.Size = new Size(75, 23);
        btnCancel.TabIndex = 5;
        btnCancel.Text = "Cancel";
        btnCancel.UseVisualStyleBackColor = true;
        //
        // Form1
        //
        AutoScaleMode = AutoScaleMode.Font;
        AcceptButton = btnOK;
        CancelButton = btnCancel;
        ClientSize = new Size(284, 121);
        Controls.Add(btnCancel);
        Controls.Add(btnOK);
        Controls.Add(txtPassword);
        Controls.Add(lblPassword);
        Controls.Add(txtUser);
        Controls.Add(lblUser);
        FormBorderStyle = FormBorderStyle.FixedDialog;
        MaximizeBox = false;
        MinimizeBox = false;
        Name = "Form1";
        StartPosition = FormStartPosition.CenterScreen;
        Text = "Log In";
        ResumeLayout(false);
        PerformLayout();
    }

    #endregion
}
