import SwiftUI
import CoreImage.CIFilterBuiltins
import UIKit
import UniformTypeIdentifiers

enum QREXColors {
    // Dynamic surfaces respect the in-app appearance preference and system contrast.
    static let background = Color(uiColor: UIColor { trait in
        trait.userInterfaceStyle == .dark
            ? UIColor(red: 3/255, green: 12/255, blue: 27/255, alpha: 1)
            : UIColor(red: 243/255, green: 246/255, blue: 1, alpha: 1)
    })
    static let card = Color(uiColor: UIColor { trait in
        trait.userInterfaceStyle == .dark
            ? UIColor(red: 16/255, green: 27/255, blue: 44/255, alpha: 1)
            : UIColor.white
    })
    static let cyan = Color(uiColor: UIColor { trait in
        trait.userInterfaceStyle == .dark
            ? UIColor(red: 16/255, green: 197/255, blue: 250/255, alpha: 1)
            : UIColor(red: 19/255, green: 92/255, blue: 195/255, alpha: 1)
    })
    static let purple = Color(red: 144/255, green: 71/255, blue: 248/255)
}

struct ResultPayload: Identifiable {
    let raw: String
    var id: String { raw }
}

// Centralized failure presentation avoids four divergent storage error dialogs.
private struct StorageFailureAlert: ViewModifier {
    @Binding var isPresented: Bool

    func body(content: Content) -> some View {
        content.alert(tr("operation_failed"), isPresented: $isPresented) {
            Button(tr("ok"), role: .cancel) {}
        }
    }
}

private extension View {
    func storageFailureAlert(_ isPresented: Binding<Bool>) -> some View {
        modifier(StorageFailureAlert(isPresented: isPresented))
    }
}

/**
 * Password-bearing Wi-Fi QR payloads must not stay indefinitely in the
 * system clipboard. Ordinary URL/text QR copies keep familiar paste behavior.
 */
@MainActor
enum QREXClipboard {
    static let sensitiveLifetime: TimeInterval = 120
    static func isSensitive(_ raw: String) -> Bool { QRContent.isWifi(raw) }

    static func copy(_ raw: String) {
        if isSensitive(raw) {
            UIPasteboard.general.setItems(
                [[UTType.utf8PlainText.identifier: raw]],
                options: [.expirationDate: Date().addingTimeInterval(sensitiveLifetime)]
            )
        } else {
            UIPasteboard.general.string = raw
        }
    }
}

enum QREXImage {
    // CIContext is costly to initialize and safe to reuse for rendering.
    private static let renderContext = CIContext()

    static func qr(_ raw: String) -> UIImage? {
        guard QRContent.isSupported(raw) else { return nil }
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(raw.utf8)
        filter.correctionLevel = "M"
        guard let image = filter.outputImage,
              let cgImage = renderContext.createCGImage(image.transformed(by: CGAffineTransform(scaleX: 10, y: 10)),
                                                  from: image.extent.applying(CGAffineTransform(scaleX: 10, y: 10)))
        else { return nil }
        return UIImage(cgImage: cgImage)
    }
}

@main
struct QREXApp: App {
    @StateObject private var store = QREXStore()

    var body: some Scene {
        WindowGroup {
            QREXRoot().environmentObject(store)
                .preferredColorScheme(store.lightMode ? .light : .dark)
                .id(store.language)
                .environment(\.locale, Locale(identifier: store.language == "system" ? Locale.current.identifier : store.language))
        }
    }
}

private enum ScreenshotFixture {
    static var enabled: Bool {
        #if DEBUG
        return ProcessInfo.processInfo.arguments.contains("-qrex-screenshot-create")
        #else
        return false
        #endif
    }
}

struct QREXRoot: View {
    @State private var tab = ScreenshotFixture.enabled ? 1 : 0

    var body: some View {
        TabView(selection: $tab) {
            ScanScreen().tabItem { Label(tr("scan"), systemImage: "qrcode.viewfinder") }.tag(0)
            CreateScreen().tabItem { Label(tr("create"), systemImage: "plus.app") }.tag(1)
            HistoryScreen().tabItem { Label(tr("history"), systemImage: "clock.arrow.circlepath") }.tag(2)
            MoreScreen().tabItem { Label(tr("more"), systemImage: "square.grid.2x2") }.tag(3)
        }
        .tint(QREXColors.cyan)
    }
}

struct ResultScreen: View {
    @EnvironmentObject private var store: QREXStore
    @Environment(\.openURL) private var openURL
    @Environment(\.dismiss) private var dismiss
    let raw: String
    @State private var showPassword = false
    @State private var showQR = false
    @State private var confirmSaveWifi = false
    @State private var showPNGExporter = false
    @State private var pngDocument: QREXPNGDocument?
    @State private var operationFailed = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 58)).foregroundStyle(QREXColors.cyan)
                    Text(tr("scan_complete")).font(.title2.bold())
                    VStack(alignment: .leading, spacing: 12) {
                        if QRContent.isWifi(raw) {
                            Text("\(tr("network")): \(QRContent.wifiField(raw, key: "S") ?? "Wi-Fi")")
                                .font(.headline)
                            HStack {
                                Text(showPassword ? "\(tr("password")): \(QRContent.wifiField(raw, key: "P") ?? "")" : "\(tr("password")): ••••••••")
                                Spacer()
                                Button { showPassword.toggle() } label: {
                                    Image(systemName: showPassword ? "eye.slash" : "eye")
                                }
                                .accessibilityLabel(showPassword ? tr("hide") : tr("show"))
                            }
                        } else {
                            Text(raw).textSelection(.enabled)
                        }
                    }.frame(maxWidth: .infinity, alignment: .leading)
                        .padding(18).background(QREXColors.card, in: RoundedRectangle(cornerRadius: 20))
                    if let url = QRContent.safeURL(raw) {
                        Text(tr("check_url"))
                            .font(.caption).foregroundStyle(.secondary)
                        Button(tr("open_link")) { openURL(url) }
                            .buttonStyle(.borderedProminent)
                    }
                    HStack {
                        Button(tr("copy")) { QREXClipboard.copy(raw) }.buttonStyle(.bordered)
                        ShareLink(item: raw) { Label(tr("share"), systemImage: "square.and.arrow.up") }
                            .buttonStyle(.bordered)
                    }
                    Button(tr("show_qr")) { showQR = true }
                        .disabled(!QRContent.isSupported(raw))
                    Button(tr("export_png")) {
                        guard let png = QREXImage.qr(raw)?.pngData() else {
                            operationFailed = true
                            return
                        }
                        pngDocument = QREXPNGDocument(contents: png)
                        showPNGExporter = true
                    }
                    .disabled(!QRContent.isSupported(raw))
                    Button(store.items.contains(where: { $0.raw == raw && $0.saved }) ? tr("saved") : tr("save")) {
                        if QRContent.isWifi(raw) {
                            confirmSaveWifi = true
                        } else if !store.save(raw) { operationFailed = true }
                    }.disabled(store.items.contains(where: { $0.raw == raw && $0.saved }))
                        .buttonStyle(.borderedProminent)
                }
                .padding(24)
            }
            .navigationTitle(tr("scan_complete"))
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button(tr("close")) { dismiss() } } }
            .sheet(isPresented: $showQR) {
                if let image = QREXImage.qr(raw) {
                    VStack(spacing: 22) {
                        Text(tr("qr_preview")).font(.title2.bold())
                        Image(uiImage: image).interpolation(.none).resizable()
                            .scaledToFit().frame(maxWidth: 240, maxHeight: 240)
                        Button(tr("close")) { showQR = false }
                    }.padding()
                }
            }
            .fileExporter(isPresented: $showPNGExporter, document: pngDocument,
                          contentType: .png, defaultFilename: "QREX-QR") { result in
                if case .failure = result { operationFailed = true }
                pngDocument = nil
            }
            .confirmationDialog(tr("wifi_save_warning"),
                isPresented: $confirmSaveWifi) {
                    Button(tr("save")) {
                        if !store.save(raw) { operationFailed = true }
                    }
                    Button(tr("cancel"), role: .cancel) {}
                }
            .storageFailureAlert($operationFailed)
        }
    }
}

struct CreateScreen: View {
    @EnvironmentObject private var store: QREXStore
    @State private var kind = 0
    @State private var content = ScreenshotFixture.enabled
        ? "https://brendigo.com" : ""
    @State private var password = ""
    @State private var revealPassword = false
    @State private var showMore = false
    @State private var presentedResult: ResultPayload?
    @State private var renderedQR: (payload: String, image: UIImage)?
    @State private var operationFailed = false
    @State private var confirmWifiSave = false
    private var choices: [String] {
        ["url", "text", "wifi", "email", "phone", "location", "contact"].map(tr)
    }

    private var raw: String {
        let input = content.trimmingCharacters(in: .whitespacesAndNewlines)
        switch kind {
        case 0: return QRContent.safeURL(input) != nil ? input : ""
        case 1: return input
        case 2: return input.isEmpty ? "" : QRContent.wifi(ssid: input, password: password)
        case 3: return input.contains("@") && !input.contains("\n") ? "mailto:\(input)" : ""
        case 4: return input.isEmpty ? "" : "tel:\(input)"
        case 5: return input.isEmpty ? "" : "geo:\(input)"
        default: return input.isEmpty ? "" : "BEGIN:VCARD\nVERSION:3.0\nFN:\(input.replacingOccurrences(of: "\n", with: " "))\nEND:VCARD"
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 13) {
                    Text(tr("create_qr")).font(.system(size: 25, weight: .bold))
                    Text(tr("enter_details")).font(.subheadline).foregroundStyle(.secondary)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(showMore ? [0, 1, 2, 6, 3, 4, 5] : [0, 1, 2, 6], id: \.self) { i in
                                Button {
                                    kind = i
                                    content = ""
                                    password = ""
                                } label: {
                                    Text(choices[i]).font(.subheadline.weight(.semibold))
                                        .padding(.horizontal, 16).padding(.vertical, 11)
                                        .foregroundStyle(kind == i ? Color.white : QREXColors.cyan)
                                        .background(kind == i ? Color(red: 22/255, green: 119/255, blue: 1)
                                                    : QREXColors.card,
                                                    in: RoundedRectangle(cornerRadius: 12))
                                }
                                .buttonStyle(.plain)
                                .accessibilityAddTraits(kind == i ? [.isSelected] : [])
                            }
                        }
                    }
                    Button(showMore ? tr("fewer_options") : tr("more_options")) {
                        showMore.toggle()
                        if !showMore && ![0, 1, 2, 6].contains(kind) { kind = 0 }
                    }.font(.footnote)
                    TextField(choices[kind], text: $content, axis: .vertical)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .padding().background(QREXColors.card, in: RoundedRectangle(cornerRadius: 14))
                    if kind == 2 {
                        HStack {
                            Group {
                                if revealPassword {
                                    TextField(tr("optional_password"), text: $password)
                                } else {
                                    SecureField(tr("optional_password"), text: $password)
                                }
                            }
                            Button { revealPassword.toggle() } label: {
                                Image(systemName: revealPassword ? "eye.slash" : "eye")
                            }
                        }.padding().background(QREXColors.card, in: RoundedRectangle(cornerRadius: 14))
                        Text(tr("wifi_warning"))
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    if let frame = renderedQR, frame.payload == raw {
                        Image(uiImage: frame.image).interpolation(.none).resizable()
                            .scaledToFit().frame(maxWidth: 210, maxHeight: 210)
                            .padding(12).background(.white, in: RoundedRectangle(cornerRadius: 20))
                        Button(tr("save_qr")) {
                            if QRContent.isWifi(raw) { confirmWifiSave = true }
                            else if !store.save(raw) { operationFailed = true }
                        }.buttonStyle(.bordered)
                        Button(tr("create_share")) {
                            presentedResult = ResultPayload(raw: frame.payload)
                        }.buttonStyle(.borderedProminent)
                    } else {
                        VStack(spacing: 12) {
                            Image(systemName: "qrcode").font(.system(size: 35))
                            Text(tr("invalid_content"))
                        }.foregroundStyle(.secondary)
                    }
                }.padding(.horizontal, 20).padding(.top, 8).padding(.bottom, 95)
            }
            .background(QREXColors.background)
            .toolbar(.hidden, for: .navigationBar)
            .task(id: raw) {
                renderedQR = nil
                let input = raw
                guard QRContent.isSupported(input) else { return }
                // Debounce input so QR rendering cannot stall typing or scrolling.
                try? await Task.sleep(nanoseconds: 140_000_000)
                guard !Task.isCancelled else { return }
                let image = await Task.detached(priority: .userInitiated) {
                    QREXImage.qr(input)
                }.value
                guard !Task.isCancelled else { return }
                renderedQR = image.map { (payload: input, image: $0) }
            }
            .sheet(item: $presentedResult) { result in
                ResultScreen(raw: result.raw)
            }
            .confirmationDialog(tr("wifi_save_warning"),
                isPresented: $confirmWifiSave) {
                    Button(tr("save")) {
                        if !store.save(raw) { operationFailed = true }
                    }
                    Button(tr("cancel"), role: .cancel) {}
                }
            .storageFailureAlert($operationFailed)
        }
    }
}

struct HistoryScreen: View {
    @EnvironmentObject private var store: QREXStore
    @State private var search = ""
    @State private var savedOnly = false
    @State private var confirmClearHistory = false
    @State private var selected: ResultPayload?
    @State private var operationFailed = false

    private var filtered: [QRHistoryItem] {
        store.items.filter {
            (!savedOnly || $0.saved) && (search.isEmpty ||
                QRContent.title($0.raw).localizedCaseInsensitiveContains(search) ||
                (!QRContent.isWifi($0.raw) &&
                    $0.raw.localizedCaseInsensitiveContains(search)))
        }
    }

    var body: some View {
        NavigationStack {
            List {
                Toggle(tr("saved"), isOn: $savedOnly)
                ForEach(filtered) { item in
                    Button {
                        selected = ResultPayload(raw: item.raw)
                    } label: {
                        HStack {
                            Image(systemName: item.saved ? "bookmark.fill" : "qrcode")
                                .foregroundStyle(QREXColors.cyan)
                            VStack(alignment: .leading) {
                                Text(QRContent.title(item.raw)).lineLimit(2)
                                Text(item.timestamp.formatted()).font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                        }
                    }.swipeActions {
                        Button(role: .destructive) {
                            if !store.delete(item) { operationFailed = true }
                        } label: {
                            Label(tr("delete"), systemImage: "trash")
                        }
                    }
                }
            }
            .searchable(text: $search, prompt: tr("search_qr"))
            .navigationTitle(tr("history"))
            .overlay {
                if filtered.isEmpty {
                    VStack(spacing: 10) {
                        Image(systemName: "clock").font(.system(size: 34))
                        Text(tr("no_saved"))
                    }.foregroundStyle(.secondary)
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(tr("clear")) { confirmClearHistory = true }
                        .disabled(!store.items.contains(where: { !$0.saved }))
                }
            }
            .sheet(item: $selected) { item in ResultScreen(raw: item.raw) }
            .confirmationDialog(tr("clear"),
                isPresented: $confirmClearHistory, titleVisibility: .visible) {
                Button(tr("clear"), role: .destructive) {
                    if !store.clearHistory() { operationFailed = true }
                }
                Button(tr("cancel"), role: .cancel) {}
            }
            .storageFailureAlert($operationFailed)
        }
    }
}

struct MoreScreen: View {
    @EnvironmentObject private var store: QREXStore
    @State private var confirmDelete = false
    @State private var confirmExport = false
    @State private var confirmPdfExport = false
    @State private var operationFailed = false
    @State private var exporting = false
    @State private var archiveDocument: QREXZipDocument?
    @State private var pdfDocument: QREXPDFDocument?
    @State private var showFileExporter = false
    @State private var showPdfExporter = false
    private var displayLanguages: [String] { QREXStrings.supported }

    var body: some View {
        NavigationStack {
            Form {
                Section(tr("settings")) {
                    Toggle(tr("save_history"), isOn: $store.historyEnabled)
                    Toggle(tr("light_theme"), isOn: $store.lightMode)
                }
                Section(tr("language")) {
                    Picker(tr("language"), selection: $store.language) {
                        Text(tr("system")).tag("system")
                        ForEach(displayLanguages, id: \.self) { code in
                            Text(Locale(identifier: code).localizedString(forLanguageCode: code) ?? code)
                                .tag(code)
                        }
                    }
                }
                Section(tr("data_tools")) {
                    HStack {
                        Button { confirmPdfExport = true } label: {
                            Label("PDF", systemImage: "doc.text")
                        }
                        Spacer()
                        Button { confirmExport = true } label: {
                            Label("ZIP", systemImage: "doc.zipper")
                        }
                    }
                    .disabled(exporting || !store.items.contains(where: { $0.saved }))
                    Text(tr("export_archive_hint"))
                        .font(.footnote).foregroundStyle(.secondary)
                    Button(tr("delete_all"), role: .destructive) {
                        confirmDelete = true
                    }
                }
                Section(tr("about")) {
                    LabeledContent(tr("about"), value: "QREX")
                    Link(destination: URL(string: "https://brendigo.com")!) {
                        Label(tr("developed_by"), systemImage: "chevron.up.right.square")
                    }
                }
                // Legal documents live at the very bottom of Settings.
                Section(tr("privacy")) {
                    NavigationLink(tr("privacy_policy")) {
                        ScrollView {
                            VStack(alignment: .leading, spacing: 18) {
                                Text(tr("privacy_summary"))
                                Link(tr("read_full_policy"),
                                     destination: URL(string:
                                        "https://github.com/bren-wp/QREX/blob/main/docs/PRIVACY.md")!)
                            }.padding()
                        }.navigationTitle(tr("privacy_policy"))
                    }
                    NavigationLink(tr("terms_of_use")) {
                        ScrollView {
                            VStack(alignment: .leading, spacing: 18) {
                                Text(tr("terms_summary"))
                                Link(tr("terms_of_use"),
                                     destination: URL(string:
                                        "https://github.com/bren-wp/QREX/blob/main/docs/TERMS.md")!)
                            }.padding()
                        }.navigationTitle(tr("terms_of_use"))
                    }
                }
            }
            .navigationTitle(tr("more"))
            .confirmationDialog(tr("confirm_delete"),
                isPresented: $confirmDelete, titleVisibility: .visible) {
                Button(tr("delete_all"), role: .destructive) {
                    if !store.clearAll() { operationFailed = true }
                }
                Button(tr("cancel"), role: .cancel) {}
            }
            .confirmationDialog(tr("wifi_warning"),
                isPresented: $confirmPdfExport, titleVisibility: .visible) {
                Button("PDF") { exportSavedCodes(asPDF: true) }
                Button(tr("cancel"), role: .cancel) {}
            }
            .confirmationDialog(tr("export_archive_warning"),
                isPresented: $confirmExport, titleVisibility: .visible) {
                Button(tr("export_archive")) { exportSavedCodes() }
                Button(tr("cancel"), role: .cancel) {}
            }
            .fileExporter(isPresented: $showPdfExporter, document: pdfDocument,
                          contentType: .pdf, defaultFilename: "QREX-Saved-Codes") { result in
                if case .failure = result { operationFailed = true }
                pdfDocument = nil
            }
            .fileExporter(isPresented: $showFileExporter, document: archiveDocument,
                          contentType: .zip, defaultFilename: "QREX-Saved-Codes") { result in
                if case .failure = result { operationFailed = true }
                archiveDocument = nil
            }
            .storageFailureAlert($operationFailed)
        }
    }

    private func exportSavedCodes(asPDF: Bool = false) {
        // User confirmed that manually saved QR records, including any Wi-Fi
        // credentials, will be embedded in a portable ZIP/PDF document.
        let selected = store.items.filter(\.saved)
        guard !selected.isEmpty, !exporting else { return }
        exporting = true
        Task {
            do {
                let data = try await Task.detached(priority: .userInitiated) {
                    if asPDF { return try QREXDocuments.createSavedCodesPdf(selected) }
                    return try QREXDocuments.exportSavedPdfZip(selected)
                }.value
                if asPDF {
                    pdfDocument = QREXPDFDocument(contents: data)
                    showPdfExporter = true
                } else {
                    archiveDocument = QREXZipDocument(contents: data)
                    showFileExporter = true
                }
            } catch {
                operationFailed = true
            }
            exporting = false
        }
    }
}
