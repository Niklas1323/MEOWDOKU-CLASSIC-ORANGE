import Foundation
import WebKit

/// Serves the game files from the app's "web" folder at app://meowdoki/...
/// (the page uses absolute paths like /img/... and /vendor/..., so it needs a real origin).
/// The origin stays the same on every start, so the saved progress (localStorage) is kept.
final class WebAssetHandler: NSObject, WKURLSchemeHandler {

    static let scheme = "app"
    static let host = "meowdoki"
    static var startURL: URL { URL(string: "\(scheme)://\(host)/App.dc.html")! }

    private let root: URL = (Bundle.main.resourceURL ?? Bundle.main.bundleURL)
        .appendingPathComponent("web", isDirectory: true).standardizedFileURL

    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        guard let url = urlSchemeTask.request.url else {
            urlSchemeTask.didFailWithError(URLError(.badURL))
            return
        }
        var path = url.path // already percent-decoded
        if path.isEmpty || path == "/" { path = "/App.dc.html" }
        let relative = String(path.drop(while: { $0 == "/" }))
        let file = root.appendingPathComponent(relative).standardizedFileURL
        // never serve anything outside the web folder
        guard file.path.hasPrefix(root.path + "/"), let data = try? Data(contentsOf: file) else {
            respond(urlSchemeTask, url: url, status: 404, mime: "text/plain", data: Data())
            return
        }
        respond(urlSchemeTask, url: url, status: 200, mime: WebAssetHandler.mimeType(file.pathExtension), data: data)
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
        // every request is answered at once in start(), nothing to cancel
    }

    private func respond(_ task: WKURLSchemeTask, url: URL, status: Int, mime: String, data: Data) {
        let type = mime.hasPrefix("text/") || mime == "application/json" ? mime + "; charset=utf-8" : mime
        let headers = [
            "Content-Type": type,
            "Content-Length": String(data.count),
            "Cache-Control": "no-cache",
        ]
        guard let response = HTTPURLResponse(url: url, statusCode: status, httpVersion: "HTTP/1.1", headerFields: headers) else {
            task.didFailWithError(URLError(.cannotParseResponse))
            return
        }
        task.didReceive(response)
        if !data.isEmpty { task.didReceive(data) }
        task.didFinish()
    }

    static func mimeType(_ ext: String) -> String {
        switch ext.lowercased() {
        case "html", "htm": return "text/html"
        case "js", "mjs": return "text/javascript"
        case "css": return "text/css"
        case "json": return "application/json"
        case "txt": return "text/plain"
        case "svg": return "image/svg+xml"
        case "png": return "image/png"
        case "jpg", "jpeg": return "image/jpeg"
        case "webp": return "image/webp"
        case "gif": return "image/gif"
        case "ico": return "image/x-icon"
        case "woff2": return "font/woff2"
        case "woff": return "font/woff"
        case "ttf": return "font/ttf"
        case "otf": return "font/otf"
        case "mp3": return "audio/mpeg"
        case "ogg": return "audio/ogg"
        case "wav": return "audio/wav"
        case "m4a": return "audio/mp4"
        case "mp4": return "video/mp4"
        default: return "application/octet-stream"
        }
    }
}
