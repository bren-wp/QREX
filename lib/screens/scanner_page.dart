import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../core/app_store.dart';
import '../ui/qrex_theme.dart';
import 'result_page.dart';

class ScannerPage extends StatefulWidget {
  const ScannerPage({
    super.key,
    required this.store,
    this.openGalleryInitially = false,
  });

  final AppStore store;
  final bool openGalleryInitially;

  @override
  State<ScannerPage> createState() => _ScannerPageState();
}

class _ScannerPageState extends State<ScannerPage> with WidgetsBindingObserver {
  final MobileScannerController _camera = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
    facing: CameraFacing.back,
    formats: [BarcodeFormat.qrCode],
  );

  bool _handling = false;
  bool _choosingImage = false;
  double _zoom = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    if (widget.openGalleryInitially) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _fromGallery();
      });
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!_camera.value.hasCameraPermission) return;
    if (state == AppLifecycleState.resumed && !_handling && !_choosingImage) {
      _camera.start().catchError((Object _) {});
    } else if (state == AppLifecycleState.inactive ||
        state == AppLifecycleState.paused) {
      _camera.stop().catchError((Object _) {});
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _camera.dispose();
    super.dispose();
  }

  void _message(String message) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message)),
      );
    }
  }

  Future<void> _onCapture(BarcodeCapture capture) async {
    if (_handling || _choosingImage) return;
    for (final barcode in capture.barcodes) {
      final raw = barcode.rawValue;
      if (raw != null && raw.trim().isNotEmpty) {
        await _showResult(raw);
        return;
      }
    }
  }

  Future<void> _showResult(String raw) async {
    if (_handling || !mounted) return;
    _handling = true;
    try {
      await _camera.stop();
      try {
        await widget.store.record(raw);
      } catch (_) {
        _message('Povijest nije moguće spremiti, ali kod možeš pregledati.');
      }
      if (!mounted) return;
      await Navigator.of(context).push<void>(
        MaterialPageRoute(builder: (_) => ResultPage(store: widget.store, raw: raw)),
      );
    } catch (_) {
      _message('Skeniranje nije uspjelo. Pokušaj ponovno.');
    } finally {
      _handling = false;
      if (mounted && !_choosingImage) {
        try {
          await _camera.start();
        } catch (_) {
          _message('Kamera trenutačno nije dostupna.');
        }
      }
    }
  }

  Future<void> _fromGallery() async {
    if (_handling || _choosingImage) return;
    _choosingImage = true;
    try {
      final selected = await ImagePicker().pickImage(source: ImageSource.gallery);
      if (selected == null || !mounted) return;
      final result = await _camera.analyzeImage(selected.path);
      if (!mounted) return;
      final barcodes = result?.barcodes ?? [];
      String? value;
      for (final code in barcodes) {
        if (code.format == BarcodeFormat.qrCode && code.rawValue?.isNotEmpty == true) {
          value = code.rawValue;
          break;
        }
      }
      if (value != null) {
        await _showResult(value);
      } else {
        _message('Na fotografiji nije pronađen QR kod.');
      }
    } catch (_) {
      _message('Fotografiju nije moguće otvoriti ili pročitati.');
    } finally {
      _choosingImage = false;
      if (mounted && !_handling) {
        try {
          await _camera.start();
        } catch (_) {
          // A camera permission error is rendered by MobileScanner.
        }
      }
    }
  }

  Future<void> _setZoom(double zoom) async {
    try {
      await _camera.setZoomScale(zoom);
      if (mounted) setState(() => _zoom = zoom);
    } catch (_) {
      _message('Zumiranje nije dostupno na ovom uređaju.');
    }
  }

  Future<void> _toggleTorch() async {
    try {
      await _camera.toggleTorch();
    } catch (_) {
      _message('Svjetiljka nije dostupna na ovom uređaju.');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Stack(fit: StackFit.expand, children: [
      MobileScanner(
        controller: _camera,
        onDetect: _onCapture,
        fit: BoxFit.cover,
        errorBuilder: (context, error) => const ColoredBox(
          color: QrexPalette.base,
          child: Center(child: Padding(
            padding: EdgeInsets.all(25),
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              Icon(Icons.no_photography_outlined, size: 52, color: QrexPalette.muted),
              SizedBox(height: 14),
              Text('Omogući kameru u postavkama uređaja i pokušaj ponovno.',
                textAlign: TextAlign.center, style: TextStyle(color: Colors.white)),
            ]),
          )),
        ),
      ),
      const IgnorePointer(child: _ScannerShade()),
      SafeArea(child: Column(children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 12, 18, 4),
            child: Row(children: [
              ValueListenableBuilder<MobileScannerState>(
                valueListenable: _camera,
                builder: (context, state, _) => IconButton.filledTonal(
                  tooltip: 'Svjetiljka',
                  onPressed: state.isRunning ? _toggleTorch : null,
                  icon: Icon(state.torchState == TorchState.on
                    ? Icons.flash_on_rounded : Icons.flash_off_rounded),
                ),
              ),
              const Expanded(child: Center(child: QrexWordmark(small: true))),
              IconButton.filledTonal(
                tooltip: 'Skeniraj s fotografije',
                onPressed: _fromGallery,
                icon: const Icon(Icons.photo_library_outlined),
              ),
            ]),
          ),
          Expanded(child: LayoutBuilder(builder: (context, available) {
            final size = math.min(310.0, math.min(
              MediaQuery.sizeOf(context).width * .73,
              available.maxHeight * .86,
            ));
            return Center(child: SizedBox(
              width: size, height: size,
              child: const CustomPaint(painter: _CornersPainter()),
            ));
          })),
          Container(
            decoration: BoxDecoration(
              color: const Color(0xDD091428),
              border: Border.all(color: Colors.white24),
              borderRadius: BorderRadius.circular(28),
            ),
            padding: const EdgeInsets.all(3),
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              for (final setting in <({double zoom, String label})>[
                (zoom: 0, label: '1×'),
                (zoom: .5, label: 'Zum +'),
                (zoom: 1, label: 'Max'),
              ])
                Semantics(
                  button: true, selected: _zoom == setting.zoom,
                  child: InkWell(
                    borderRadius: BorderRadius.circular(25),
                    onTap: () => _setZoom(setting.zoom),
                    child: Container(
                      width: 62, height: 38,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: _zoom == setting.zoom ? QrexPalette.primary : Colors.transparent,
                        borderRadius: BorderRadius.circular(25),
                      ),
                      child: Text(setting.label,
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
                    ),
                  ),
                ),
            ]),
          ),
          const SizedBox(height: 13),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
            decoration: BoxDecoration(
              color: const Color(0xDD091428),
              borderRadius: BorderRadius.circular(28),
              border: Border.all(color: Colors.white24),
            ),
            child: const Text('Usmjeri kameru prema QR kodu',
              style: TextStyle(color: Colors.white, fontSize: 13)),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
            child: TextButton.icon(
              onPressed: _fromGallery,
              icon: const Icon(Icons.image_outlined),
              label: const Text('Skeniraj iz slike'),
              style: TextButton.styleFrom(foregroundColor: Colors.white),
            ),
          ),
        ])),
    ]);
  }
}

class _ScannerShade extends StatelessWidget {
  const _ScannerShade();

  @override
  Widget build(BuildContext context) => const DecoratedBox(
    decoration: BoxDecoration(gradient: LinearGradient(
      begin: Alignment.topCenter,
      end: Alignment.bottomCenter,
      colors: [Color(0xDA030C1B), Colors.transparent, Colors.transparent, Color(0xDA030C1B)],
      stops: [0, .26, .69, 1],
    )),
  );
}

class _CornersPainter extends CustomPainter {
  const _CornersPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = const Color(0xFF58B9FF)
      ..strokeWidth = 4
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;
    final extent = math.min(36.0, size.width * .19);
    final w = size.width, h = size.height;
    const inset = 16.0;
    final path = Path()
      ..moveTo(0, extent)..lineTo(0, inset)
      ..quadraticBezierTo(0, 0, inset, 0)..lineTo(extent, 0)
      ..moveTo(w - extent, 0)..lineTo(w - inset, 0)
      ..quadraticBezierTo(w, 0, w, inset)..lineTo(w, extent)
      ..moveTo(0, h - extent)..lineTo(0, h - inset)
      ..quadraticBezierTo(0, h, inset, h)..lineTo(extent, h)
      ..moveTo(w - extent, h)..lineTo(w - inset, h)
      ..quadraticBezierTo(w, h, w, h - inset)..lineTo(w, h - extent);
    canvas.drawPath(path, paint);
    if (w > 70) {
      canvas.drawLine(Offset(6, h / 2), Offset(w - 6, h / 2),
        Paint()
          ..shader = const LinearGradient(colors: [QrexPalette.cyan, QrexPalette.purple])
            .createShader(Rect.fromLTWH(0, h / 2, w, 1))
          ..strokeWidth = 2);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
