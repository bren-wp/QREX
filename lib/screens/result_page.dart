import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../core/app_store.dart';
import '../core/qr_payload.dart';
import '../ui/qrex_theme.dart';

class ResultPage extends StatefulWidget {
  const ResultPage({super.key, required this.store, required this.raw});
  final AppStore store;
  final String raw;
  @override
  State<ResultPage> createState() => _ResultPageState();
}

class _ResultPageState extends State<ResultPage> {
  bool _busy = false;
  bool _revealWifi = false;

  Future<void> _open() async {
    final uri = actionForPayload(widget.raw);
    if (uri == null) return;
    setState(() => _busy = true);
    try {
      if (!await launchUrl(uri, mode: LaunchMode.externalApplication)) {
        _message('Na uređaju nema aplikacije za ovu radnju.');
      }
    } catch (_) {
      _message('Nije moguće otvoriti sadržaj.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _message(String text) {
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
  }

  Future<void> _copy() async {
    try {
      await Clipboard.setData(ClipboardData(text: widget.raw));
      _message('Kopirano.');
    } catch (_) {
      _message('Kopiranje nije uspjelo.');
    }
  }

  Future<void> _share() async {
    try {
      final renderBox = context.findRenderObject() as RenderBox?;
      await SharePlus.instance.share(ShareParams(
        text: widget.raw,
        sharePositionOrigin: renderBox == null ? null : renderBox.localToGlobal(Offset.zero) & renderBox.size,
      ));
    } catch (_) {
      _message('Dijeljenje nije uspjelo.');
    }
  }

  void _showQr() {
    if (!fitsQrPayload(widget.raw)) {
      _message('Sadržaj je predugačak za prikaz QR koda.');
      return;
    }
    showModalBottomSheet<void>(context: context, showDragHandle: true,
      isScrollControlled: true,
      builder: (dialogContext) {
        final viewport = MediaQuery.sizeOf(dialogContext);
        final preferredSide = (viewport.height * .35).clamp(0.0, 220.0).toDouble();
        final qrSide = responsiveQrSide(
          viewport.width, preferredSide: preferredSide, horizontalInset: 84);
        return SafeArea(child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const Text('QR kod', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
          const SizedBox(height: 20),
          Container(padding: const EdgeInsets.all(18), color: Colors.white,
            child: QrImageView(data: widget.raw, size: qrSide,
              version: QrVersions.auto, errorCorrectionLevel: QrErrorCorrectLevel.M)),
          const SizedBox(height: 14),
          TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('Zatvori')),
        ]),
      ));
      },
    );
  }

  Future<void> _save() async {
    if (_busy) return;
    setState(() => _busy = true);
    try {
      await widget.store.save(widget.raw);
      _message('Spremljeno na uređaj.');
    } catch (_) {
      _message('Spremanje nije uspjelo. Provjeri slobodan prostor na uređaju.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final kind = detectKind(widget.raw);
    final action = actionForPayload(widget.raw);
    final link = kind == QrKind.link;
    final saved = widget.store.isSaved(widget.raw);
    return Scaffold(
      appBar: AppBar(title: const Text('Rezultat')),
      body: SafeArea(child: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          const SizedBox(height: 25),
          Center(child: Container(
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              color: QrexPalette.primary.withValues(alpha: .16),
              borderRadius: BorderRadius.circular(24)),
            child: Icon(iconForKind(kind), size: 43, color: QrexPalette.cyan),
          )),
          const SizedBox(height: 20),
          Text('Skeniranje završeno', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold),
            textAlign: TextAlign.center),
          const SizedBox(height: 6),
          Text(kind.label, style: const TextStyle(color: QrexPalette.muted), textAlign: TextAlign.center),
          const SizedBox(height: 25),
          Card(child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Icon(iconForKind(kind), color: QrexPalette.primary),
                const SizedBox(width: 10),
                Text(kind.label, style: const TextStyle(fontWeight: FontWeight.bold)),
              ]),
              const SizedBox(height: 16),
              if (kind == QrKind.wifi) ...[
                Text('Mreža: ${wifiField(widget.raw, 'S') ?? 'Nepoznato'}',
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                Row(children: [
                  Expanded(child: Text(_revealWifi
                    ? 'Lozinka: ${wifiField(widget.raw, 'P')?.isNotEmpty == true ? wifiField(widget.raw, 'P') : 'Otvorena mreža'}'
                    : 'Lozinka: ••••••••')),
                  IconButton(tooltip: _revealWifi ? 'Sakrij lozinku' : 'Prikaži lozinku',
                    onPressed: () => setState(() => _revealWifi = !_revealWifi),
                    icon: Icon(_revealWifi ? Icons.visibility_off_outlined : Icons.visibility_outlined)),
                ]),
                const SizedBox(height: 5),
                const Text('Wi-Fi sadržaj nije automatski spremljen u povijest.',
                  style: TextStyle(fontSize: 12, color: QrexPalette.muted)),
              ] else
                SelectableText(widget.raw, style: const TextStyle(fontSize: 17, height: 1.45)),
              if (link) ...[
                const SizedBox(height: 14),
                const Text('Provjeri adresu prije otvaranja. Sigurnost poveznice nije provjerena.',
                  style: TextStyle(fontSize: 12, color: QrexPalette.muted)),
              ],
            ]),
          )),
          const SizedBox(height: 18),
          if (action != null) ...[
            QrexButton(label: switch (kind) {
              QrKind.link => 'Otvori poveznicu',
              QrKind.email => 'Pošalji e-mail',
              QrKind.phone => 'Nazovi broj',
              QrKind.location => 'Otvori kartu',
              _ => 'Otvori',
            }, icon: Icons.open_in_new_rounded, onPressed: _busy ? null : _open),
            const SizedBox(height: 10),
          ],
          Row(children: [
            Expanded(child: OutlinedButton.icon(onPressed: _copy,
              icon: const Icon(Icons.copy_rounded), label: const Text('Kopiraj'))),
            const SizedBox(width: 10),
            Expanded(child: OutlinedButton.icon(onPressed: _share,
              icon: const Icon(Icons.share_outlined), label: const Text('Podijeli'))),
          ]),
          const SizedBox(height: 11),
          QrexButton(label: 'Prikaži QR kod', secondary: true,
            icon: Icons.qr_code_2_outlined,
            onPressed: fitsQrPayload(widget.raw) ? _showQr : null),
          const SizedBox(height: 11),
          if (kind == QrKind.wifi)
            const Padding(padding: EdgeInsets.only(bottom: 12),
              child: Text('Spremanjem Wi-Fi koda spremit će se i lozinka u lokalnu pohranu.',
                style: TextStyle(color: QrexPalette.muted, fontSize: 12))),
          QrexButton(label: saved ? 'Spremljeno' : 'Spremi u aplikaciju',
            secondary: true, icon: saved ? Icons.bookmark : Icons.bookmark_outline,
            onPressed: saved || _busy ? null : _save),
        ],
      )),
    );
  }
}

IconData iconForKind(QrKind kind) => switch (kind) {
  QrKind.link => Icons.link_rounded,
  QrKind.wifi => Icons.wifi_rounded,
  QrKind.contact => Icons.person_outline_rounded,
  QrKind.email => Icons.mail_outline_rounded,
  QrKind.phone => Icons.call_outlined,
  QrKind.text => Icons.description_outlined,
  QrKind.location => Icons.place_outlined,
  QrKind.event => Icons.event_outlined,
};
