#!/usr/bin/env python3
"""Builds the sample projects in samples/ from the Visual Studio "Windows Forms App" (.NET 8) template, by driving
Formwork.Host with designer edits - so every Form1.Designer.cs is exactly what the designer writes.
Usage: python3 tools/make_samples.py   (needs ./gradlew publishHost first)"""
import json, os, shutil, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HOST = os.path.join(ROOT, 'build/host/linux-x64/Formwork.Host.dll')
DOTNET = next((p for p in ['/opt/dotnet/dotnet', '/usr/share/dotnet/dotnet', '/usr/lib/dotnet/dotnet'] if os.path.exists(p)), 'dotnet')
TEMPLATE = os.path.join(ROOT, 'samples/VsTemplate.Net8/WinFormsApp1')

class Host:
    def __init__(self):
        self.p = subprocess.Popen([DOTNET, HOST], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, encoding='utf-8')
        self.n = 0
    def call(self, method, params=None, files=None):
        self.n += 1
        msg = {'id': self.n, 'method': method}
        if params is not None: msg['params'] = params
        if files is not None: msg['files'] = files
        self.p.stdin.write(json.dumps(msg) + '\n'); self.p.stdin.flush()
        r = json.loads(self.p.stdout.readline())
        if 'error' in r: raise RuntimeError(f"{method}: {r['error']}")
        return r
    def close(self):
        try: self.call('close')
        except Exception: pass
        self.p.wait(5)

def read(path):
    if not os.path.exists(path): return None
    with open(path, encoding='utf-8-sig', newline='') as f: return f.read().replace('\r\n', '\n')

def write(path, text):
    """Like Visual Studio: CRLF, and a BOM on Designer.cs and .resx (Form1.cs keeps what it had: none)."""
    bom = path.endswith('.Designer.cs') or path.endswith('.resx')
    with open(path, 'w', encoding='utf-8-sig' if bom else 'utf-8', newline='') as f: f.write(text.replace('\n', '\r\n'))

def build(name, project, steps, form='Form1', rename_project=None):
    target = os.path.join(ROOT, 'samples', name)
    if os.path.exists(target): shutil.rmtree(target)
    proj_dir = os.path.join(target, project)
    shutil.copytree(TEMPLATE, proj_dir)
    os.remove(os.path.join(proj_dir, 'WinFormsApp1.csproj.user'))
    if project != 'WinFormsApp1':
        os.rename(os.path.join(proj_dir, 'WinFormsApp1.csproj'), os.path.join(proj_dir, project + '.csproj'))
        for f in os.listdir(proj_dir):
            p = os.path.join(proj_dir, f)
            if f.endswith('.cs'):
                t = read(p); write_plain = not f.endswith('.Designer.cs')
                t = t.replace('namespace WinFormsApp1', 'namespace ' + project)
                with open(p, 'w', encoding='utf-8' if write_plain else 'utf-8-sig', newline='') as fh: fh.write(t.replace('\n', '\r\n'))
    designer = os.path.join(proj_dir, form + '.Designer.cs')
    files = lambda: {os.path.join(proj_dir, form + ext): read(os.path.join(proj_dir, form + ext)) for ext in ['.Designer.cs', '.cs', '.resx']}
    h = Host()
    try:
        h.call('open', {'path': designer}, files())
        for ops in steps:
            r = h.call('apply', {'ops': ops}, files())
            for path, text in (r.get('writes') or {}).items(): write(path, text)
    finally:
        h.close()
    print('built', name)
    return proj_dir

def op(kind, **kw): return dict(op=kind, **kw)
def add(type_, id_, x, y, w=None, h=None, parent=''):
    d = op('add', type='System.Windows.Forms.' + type_, parent=parent, x=x, y=y, id=id_)
    if w: d['w'] = w
    if h: d['h'] = h
    return d
def prop(id_, p, v): return op('setProp', id=id_, prop=p, value=v)
def event(id_, e, handler): return op('setEvent', id=id_, event=e, handler=handler)

def main():
    # --- Calculator: a TextBox display and a 4x4 grid of buttons -----------------------------------------------
    keys = ['7', '8', '9', '/', '4', '5', '6', '*', '1', '2', '3', '-', '0', '.', '=', '+']
    names = {'/': 'Divide', '*': 'Multiply', '-': 'Minus', '+': 'Plus', '.': 'Point', '=': 'Equals'}
    calc = [[add('TextBox', 'txtDisplay', 12, 12, 236, 23), prop('txtDisplay', 'TextAlign', 'Right'), prop('txtDisplay', 'ReadOnly', 'True'), prop('txtDisplay', 'Text', '0')]]
    grid = []
    for i, k in enumerate(keys):
        n = 'btn' + names.get(k, k)
        grid += [add('Button', n, 12 + (i % 4) * 60, 45 + (i // 4) * 45, 55, 40), prop(n, 'Text', k)]
    calc.append(grid)
    calc.append([event('btn' + names.get(k, k), 'Click', 'Digit_Click') for k in keys if k.isdigit() or k == '.'])
    calc.append([event('btn' + names[k], 'Click', 'Operator_Click') for k in '/*-+'] + [event('btnEquals', 'Click', 'btnEquals_Click')])
    calc.append([op('setForm', w=260, h=230), prop('', 'Text', 'Calculator'), prop('', 'FormBorderStyle', 'FixedSingle'), prop('', 'MaximizeBox', 'False')])
    build('Calculator', 'Calculator', calc)

    # --- Login form: labels, text boxes, AcceptButton / CancelButton ------------------------------------------
    login = [[add('Label', 'lblUser', 12, 15), prop('lblUser', 'Text', 'Username:'), add('TextBox', 'txtUser', 90, 12, 180, 23),
              add('Label', 'lblPassword', 12, 48), prop('lblPassword', 'Text', 'Password:'), add('TextBox', 'txtPassword', 90, 45, 180, 23),
              prop('txtPassword', 'PasswordChar', '*'),
              add('Button', 'btnOK', 114, 85), prop('btnOK', 'Text', 'OK'), add('Button', 'btnCancel', 195, 85), prop('btnCancel', 'Text', 'Cancel'),
              prop('btnCancel', 'DialogResult', 'Cancel')],
             [prop('', 'AcceptButton', 'btnOK'), prop('', 'CancelButton', 'btnCancel'), prop('', 'Text', 'Log In'), op('setForm', w=284, h=121),
              prop('', 'FormBorderStyle', 'FixedDialog'), prop('', 'StartPosition', 'CenterScreen'), prop('', 'MaximizeBox', 'False'), prop('', 'MinimizeBox', 'False')],
             [event('btnOK', 'Click', 'btnOK_Click')]]
    build('LoginForm', 'LoginForm', login)

    # --- Timer + PictureBox ---------------------------------------------------------------------------------
    timer = [[add('PictureBox', 'pictureBox1', 12, 12, 200, 150), prop('pictureBox1', 'BorderStyle', 'FixedSingle'), prop('pictureBox1', 'BackColor', 'White'),
              add('Label', 'lblTime', 12, 175), prop('lblTime', 'Text', '00:00:00'), prop('lblTime', 'Font', 'Segoe UI, 14.25pt'),
              op('add', type='System.Windows.Forms.Timer', id='timer1'), prop('timer1', 'Interval', '1000'), prop('timer1', 'Enabled', 'True')],
             [event('timer1', 'Tick', 'timer1_Tick'), event('', 'Load', 'Form1_Load')],
             [prop('', 'Text', 'Timer and Picture'), op('setForm', w=224, h=215)]]
    build('TimerPicture', 'TimerPicture', timer)

    # --- Tabs demo: TabControl, GroupBox, Panel, TableLayoutPanel, FlowLayoutPanel, SplitContainer ------------
    tabs = [[add('TabControl', 'tabControl1', 12, 12, 560, 330)],
            [op('setItems', id='tabControl1', prop='TabPages', values=['Boxes', 'Layouts'], ids=['tabPage1', 'tabPage2'])],
            [add('GroupBox', 'groupBox1', 6, 6, 250, 120, parent='tabPage1'), prop('groupBox1', 'Text', 'Options'),
             add('Panel', 'panel1', 270, 6, 270, 120, parent='tabPage1'), prop('panel1', 'BorderStyle', 'FixedSingle'),
             add('SplitContainer', 'splitContainer1', 6, 135, 534, 155, parent='tabPage1')],
            [add('CheckBox', 'checkBox1', 12, 24, parent='groupBox1'), add('RadioButton', 'radioButton1', 12, 50, parent='groupBox1'),
             add('Button', 'button1', 12, 12, parent='panel1'),
             add('ListBox', 'listBox1', 6, 6, 120, 120, parent='splitContainer1.Panel1'), add('TextBox', 'textBox1', 6, 6, 200, 23, parent='splitContainer1.Panel2')],
            [add('TableLayoutPanel', 'tableLayoutPanel1', 6, 6, 260, 150, parent='tabPage2'), add('FlowLayoutPanel', 'flowLayoutPanel1', 280, 6, 260, 150, parent='tabPage2')],
            [add('Label', 'label1', 0, 0, parent='tableLayoutPanel1'), add('Button', 'button2', 0, 0, parent='flowLayoutPanel1'), add('Button', 'button3', 0, 0, parent='flowLayoutPanel1')],
            [prop('', 'Text', 'Tabs and Containers'), op('setForm', w=584, h=354)]]
    build('TabsDemo', 'TabsDemo', tabs)

    # --- Menu demo: MenuStrip, ContextMenuStrip, StatusStrip, OpenFileDialog -----------------------------
    menu = [[add('MenuStrip', 'menuStrip1', 0, 0), add('StatusStrip', 'statusStrip1', 0, 0),
             op('add', type='System.Windows.Forms.ContextMenuStrip', id='contextMenuStrip1'), op('add', type='System.Windows.Forms.OpenFileDialog', id='openFileDialog1'),
             add('TextBox', 'textBox1', 12, 36, 360, 200)],
            [op('setItems', id='menuStrip1', prop='Items', values=['&File', '&Help'], ids=['', ''])],
            [op('setItems', id='fileToolStripMenuItem', prop='DropDownItems', values=['&Open...', '-', 'E&xit'], ids=['', '', '']),
             op('setItems', id='helpToolStripMenuItem', prop='DropDownItems', values=['&About'], ids=['']),
             op('setItems', id='statusStrip1', prop='Items', values=['Ready'], ids=['+ToolStripStatusLabel']),
             op('setItems', id='contextMenuStrip1', prop='Items', values=['Cu&t', '&Copy', '&Paste'], ids=['', '', ''])],
            [prop('textBox1', 'Multiline', 'True'), prop('textBox1', 'ContextMenuStrip', 'contextMenuStrip1'), op('setBounds', id='textBox1', x=12, y=36, w=360, h=200)],
            [event('openToolStripMenuItem', 'Click', 'openToolStripMenuItem_Click'), event('exitToolStripMenuItem', 'Click', 'exitToolStripMenuItem_Click')],
            [prop('', 'Text', 'Menu Demo'), op('setForm', w=384, h=271)]]
    build('MenuDemo', 'MenuDemo', menu)

    # --- Two forms: Form1 opens Form2 ------------------------------------------------------------------
    two = build('TwoForms', 'TwoForms', [[add('Button', 'btnOpen', 20, 20, 120, 30), prop('btnOpen', 'Text', 'Open Form2')], [event('btnOpen', 'Click', 'btnOpen_Click')]])
    add_second_form(two, 'TwoForms')


def add_second_form(proj_dir, namespace):
    """Form2 next to Form1, as Visual Studio's Add → Form (Windows Forms) makes it, then a label on it."""
    for ext in ['.cs', '.Designer.cs']:
        t = read(os.path.join(TEMPLATE, 'Form1' + ext)).replace('Form1', 'Form2').replace('namespace WinFormsApp1', 'namespace ' + namespace)
        p = os.path.join(proj_dir, 'Form2' + ext)
        with open(p, 'w', encoding='utf-8-sig' if ext == '.Designer.cs' else 'utf-8', newline='') as f: f.write(t.replace('\n', '\r\n'))
    designer = os.path.join(proj_dir, 'Form2.Designer.cs')
    h = Host()
    try:
        h.call('open', {'path': designer})
        files = {os.path.join(proj_dir, 'Form2' + e): read(os.path.join(proj_dir, 'Form2' + e)) for e in ['.Designer.cs', '.cs', '.resx']}
        r = h.call('apply', {'ops': [add('Label', 'label1', 20, 20), prop('label1', 'Text', 'This is Form2'), op('setForm', w=300, h=150)]}, files)
        for path, text in (r.get('writes') or {}).items(): write(path, text)
    finally:
        h.close()

if __name__ == "__main__":
    main()
