import AVFoundation
import SwiftUI
import PhotosUI
import Vision
import UIKit
import ImageIO

struct CameraScanner: UIViewRepresentable {
    let onRead: (String) -> Void
    @Binding var torchOn: Bool

    func makeCoordinator() -> Coordinator { Coordinator(onRead: onRead) }

    func makeUIView(context: Context) -> CameraView {
        let view = CameraView()
        let coordinator = context.coordinator
        coordinator.preview = view.preview
        coordinator.start()
        return view
    }

    func updateUIView(_ uiView: CameraView, context: Context) {
        context.coordinator.setTorch(torchOn)
    }

    static func dismantleUIView(_ uiView: CameraView, coordinator: Coordinator) {
        coordinator.stop()
    }

    final class CameraView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var preview: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }

    final class Coordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
        let session = AVCaptureSession()
        let queue = DispatchQueue(label: "com.brendigo.qrex.camera", qos: .userInitiated)
        let onRead: (String) -> Void
        weak var preview: AVCaptureVideoPreviewLayer?
        private var consumed = false
        private var configured = false

        init(onRead: @escaping (String) -> Void) { self.onRead = onRead }

        func start() {
            queue.async { [weak self] in
                guard let self else { return }
                guard AVCaptureDevice.authorizationStatus(for: .video) == .authorized else { return }
                if !self.configured {
                    guard let device = AVCaptureDevice.default(for: .video),
                          let input = try? AVCaptureDeviceInput(device: device) else { return }
                    self.session.beginConfiguration()
                    defer { self.session.commitConfiguration() }
                    guard self.session.canAddInput(input) else { return }
                    self.session.addInput(input)
                    let output = AVCaptureMetadataOutput()
                    guard self.session.canAddOutput(output) else { return }
                    self.session.addOutput(output)
                    output.setMetadataObjectsDelegate(self, queue: .main)
                    output.metadataObjectTypes = [.qr]
                    self.configured = true
                }
                DispatchQueue.main.async { [weak self] in
                    self?.preview?.session = self?.session
                    self?.preview?.videoGravity = .resizeAspectFill
                }
                if !self.session.isRunning { self.session.startRunning() }
            }
        }

        func stop() {
            queue.async { [weak self] in
                guard let self else { return }
                // Camera permission and torch should never outlive this view.
                self.configureTorch(false)
                if self.session.isRunning { self.session.stopRunning() }
            }
        }

        private func configureTorch(_ enabled: Bool) {
            guard let device = AVCaptureDevice.default(for: .video),
                  device.hasTorch,
                  (try? device.lockForConfiguration()) != nil else { return }
            defer { device.unlockForConfiguration() }
            device.torchMode = enabled ? .on : .off
        }

        func setTorch(_ enabled: Bool) {
            queue.async { [weak self] in
                self?.configureTorch(enabled)
            }
        }

        func metadataOutput(_ output: AVCaptureMetadataOutput,
                            didOutput objects: [AVMetadataObject],
                            from connection: AVCaptureConnection) {
            guard !consumed, let code = objects.compactMap({ $0 as? AVMetadataMachineReadableCodeObject })
                .first(where: { $0.type == .qr })?.stringValue, !code.isEmpty else { return }
            consumed = true
            onRead(code)
        }
    }
}

struct ScanScreen: View {
    @EnvironmentObject private var store: QREXStore
    @Environment(\.scenePhase) private var scenePhase
    @State private var access: AVAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var code: String?
    @State private var torch = false
    @State private var message: String?
    @State private var cameraRevision = 0

    var body: some View {
        ZStack {
            QREXColors.background.ignoresSafeArea()
            VStack(spacing: 20) {
                Text("QREX").font(.system(size: 30, weight: .black, design: .rounded))
                    .foregroundStyle(QREXColors.cyan)
                Text(tr("scan_qr")).font(.title2.bold()).foregroundStyle(.white)
                ZStack {
                    if access == .authorized && scenePhase == .active && code == nil {
                        CameraScanner(onRead: receive, torchOn: $torch)
                            .id(cameraRevision)
                            .clipShape(RoundedRectangle(cornerRadius: 28))
                    } else if access == .authorized {
                        RoundedRectangle(cornerRadius: 28)
                            .fill(QREXColors.card)
                    } else {
                        RoundedRectangle(cornerRadius: 28)
                            .fill(QREXColors.card)
                        VStack(spacing: 12) {
                            Image(systemName: "camera.fill").font(.system(size: 36))
                            Text(tr("allow_camera"))
                                .multilineTextAlignment(.center)
                            Button(tr("enable_camera")) { requestCamera() }
                                .buttonStyle(.borderedProminent)
                        }.foregroundStyle(.white).padding()
                    }
                    RoundedRectangle(cornerRadius: 28)
                        .stroke(QREXColors.cyan, lineWidth: 3)
                        .padding(16).allowsHitTesting(false)
                }
                .frame(maxHeight: 350)
                .padding(.horizontal, 22)
                HStack(spacing: 20) {
                    Button {
                        torch.toggle()
                    } label: {
                        Label(tr("flashlight"), systemImage: torch ? "flashlight.on.fill" : "flashlight.off.fill")
                    }.buttonStyle(.bordered).disabled(access != .authorized || scenePhase != .active || code != nil)
                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                        Label(tr("from_gallery"), systemImage: "photo")
                    }.buttonStyle(.bordered)
                }.tint(QREXColors.cyan)
                Text(tr("point_camera"))
                    .font(.footnote).foregroundStyle(.gray)
            }.padding(.vertical, 18)
        }
        .task { requestCamera() }
        .onChange(of: selectedPhoto) { item in
            Task { await readPhoto(item) }
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                cameraRevision += 1
            } else {
                torch = false
            }
        }
        .onDisappear {
            torch = false
        }
        .sheet(item: Binding(
            get: { code.map { ResultPayload(raw: $0) } },
            set: { if $0 == nil { code = nil; torch = false; cameraRevision += 1 } }
        )) { item in
            ResultScreen(raw: item.raw)
        }
        .alert("QREX", isPresented: Binding(
            get: { message != nil },
            set: { if !$0 { message = nil } }
        )) {
            Button(tr("ok"), role: .cancel) { message = nil }
        } message: { Text(message ?? "") }
    }

    private func requestCamera() {
        if AVCaptureDevice.authorizationStatus(for: .video) == .notDetermined {
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    access = granted ? .authorized : .denied
                }
            }
        } else {
            access = AVCaptureDevice.authorizationStatus(for: .video)
        }
    }

    private func receive(_ value: String) {
        guard code == nil else { return }
        store.record(value)
        code = value
    }

    private func readPhoto(_ item: PhotosPickerItem?) async {
        guard let item else { return }
        // Reset even on a failed or oversized image, so the same photo can be selected again.
        defer { selectedPhoto = nil }
        do {
            guard let data = try await item.loadTransferable(type: Data.self) else {
                message = tr("image_failed"); return
            }
            // Limit oversized images to avoid avoidable memory pressure.
            guard data.count < 20_000_000,
                  let source = CGImageSourceCreateWithData(data as CFData, nil),
                  let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [String: Any],
                  let width = properties[kCGImagePropertyPixelWidth as String] as? Int,
                  let height = properties[kCGImagePropertyPixelHeight as String] as? Int,
                  width > 0, height > 0,
                  Int64(width) * Int64(height) <= 24_000_000 else {
                message = tr("image_failed"); return
            }
            let request = VNDetectBarcodesRequest()
            request.symbologies = [.qr]
            try await Task.detached(priority: .userInitiated) {
                try VNImageRequestHandler(data: data).perform([request])
            }.value
            if let raw = request.results?.compactMap(\.payloadStringValue).first {
                receive(raw)
            } else {
                message = tr("qr_not_found")
            }
        } catch {
            message = tr("image_failed")
        }
    }
}
