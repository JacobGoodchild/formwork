namespace CustomControls;

/// <summary>A small control of the project's own, as students write them: draws a score in a box.</summary>
public class ScoreBox : Control
{
    private int _score;

    public ScoreBox()
    {
        Size = new Size(120, 40);
    }

    public int Score
    {
        get => _score;
        set { _score = value; Invalidate(); }
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        base.OnPaint(e);
        e.Graphics.FillRectangle(Brushes.LightYellow, ClientRectangle);
        e.Graphics.DrawRectangle(Pens.DarkOrange, 0, 0, Width - 1, Height - 1);
        TextRenderer.DrawText(e.Graphics, "Score: " + _score, Font, ClientRectangle, Color.Black,
            TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }
}

/// <summary>A control with a bug in its drawing code: the designer must show a red cross, not fall over.</summary>
public class BrokenControl : Control
{
    public BrokenControl()
    {
        Size = new Size(120, 40);
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        throw new InvalidOperationException("BrokenControl's OnPaint has a bug.");
    }
}
