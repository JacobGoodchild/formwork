using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.Json.Nodes;
using NetForms.Design;

namespace Formwork.Host;

/// <summary>
/// One JSON request per line on stdin, one JSON response per line on stdout - NetForms' DesignerProtocol,
/// with two Formwork additions (docs/PROTOCOL.md):
/// <list type="bullet">
/// <item>a request may carry <c>"files": {"/abs/path": "text" | null}</c>: what Rider's editor holds for the
/// form's files (null: the file does not exist). The designer reads those instead of the disk.</item>
/// <item>a response carries <c>"writes": {"/abs/path": "text"}</c> when the request changed a file. Nothing is
/// written to disk by this process; Rider applies the writes to its documents.</item>
/// </list>
/// </summary>
public static class Program
{
    private static readonly Dictionary<string, string?> s_buffers = new(StringComparer.Ordinal);
    private static readonly Dictionary<string, string> s_writes = new(StringComparer.Ordinal);

    public static int Main(string[] args)
    {
        var utf8 = new UTF8Encoding(encoderShouldEmitUTF8Identifier: false);
        Console.InputEncoding = utf8;
        var protocolOut = new StreamWriter(Console.OpenStandardOutput(), utf8) { AutoFlush = true, NewLine = "\n" };
        // Anything a control prints goes to stderr, so it can never corrupt the protocol stream.
        Console.SetOut(Console.Error);

        WindowsPalette.Install();
        WindowsPalette.InstallFonts();
        DesignSurface.ReadFileOverride = Read;
        using var protocol = new DesignerProtocol();
        // Only the open form's own files may be written (the plugin checks this too).
        DesignSurface.WriteFileOverride = (path, text) =>
        {
            var surface = protocol.Surface;
            var allowed = new[] { surface?.FilePath, surface?.CompanionPath, surface?.ResxPath };
            if (Array.IndexOf(allowed, path) < 0) { Console.Error.WriteLine($"Formwork: refused a write outside the form: {path}"); return; }
            s_buffers[path] = text; s_writes[path] = text;
        };
        string? line;
        while ((line = Console.In.ReadLine()) != null)
        {
            if (string.IsNullOrWhiteSpace(line)) continue;
            protocolOut.WriteLine(Handle(protocol, line));
            if (protocol.Closed) break;
        }
        return 0;
    }

    /// <summary>Takes in the request's buffers, answers it, and adds what it wrote.</summary>
    public static string Handle(DesignerProtocol protocol, string line)
    {
        s_writes.Clear();
        string inner = line;
        try
        {
            if (JsonNode.Parse(line) is JsonObject request)
            {
                if (request["files"] is JsonObject files)
                {
                    foreach (var (path, value) in files)
                        s_buffers[Path.GetFullPath(path)] = value?.GetValue<string>();
                    request.Remove("files");
                    inner = request.ToJsonString();
                }
                if ((string?)request["method"] == "formwork.hello")
                    return new JsonObject { ["id"] = request["id"]?.DeepClone(), ["result"] = new JsonObject { ["name"] = "Formwork host", ["protocol"] = DesignerProtocol.Version, ["formwork"] = 1 } }.ToJsonString();
            }
        }
        catch (Exception)
        {
            // Not JSON: DesignerProtocol answers with its own error.
        }
        var answer = protocol.Handle(inner);
        if (s_writes.Count == 0) return answer;
        var response = JsonNode.Parse(answer)!.AsObject();
        var writes = new JsonObject();
        foreach (var (path, text) in s_writes) writes[path] = text;
        response["writes"] = writes;
        s_writes.Clear();
        return response.ToJsonString();
    }

    private static string? Read(string path)
    {
        if (s_buffers.TryGetValue(path, out var text)) return text;
        try { return File.Exists(path) ? File.ReadAllText(path) : null; }
        catch (IOException) { return null; }
    }
}
