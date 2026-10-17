import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:share_plus/share_plus.dart';

import '../core/app_store.dart';
import '../core/qr_payload.dart';
import '../ui/qrex_theme.dart';
import 'result_page.dart';

class CreatePage extends StatefulWidget {
  const CreatePage({super.key, required this.store});
  final AppStore store;
  @override
  State<CreatePage> createState() => _CreatePageState();
}

class _CreatePageState extends State<CreatePage> {
  final _fields = <String, TextEditingController>{
    for (final key in ['url', 'text', 'ssid', 'password', 'name', 'phone', 'email', 'location', 'event'])
      key: TextEditingController(),
  };
  final _qrKey = GlobalKey();
  QrKind _kind = QrKind.link;
  bool _hiddenNetwork = false;
  bool _wifiPasswordVisible = false;
  bool _showAdvanced = false;
  DateTime _start = DateTime.now().add(const Duration(days: 1));
  DateTime _end = DateTime.now().add(const Duration(days: 1, hours: 1));
  bool _busy = false;

  @override
  void dispose() {
    for (final field in _fields.values) { field.dispose(); }
    super.dispose();
  }

  String _get(String name) => _fields[name]!.text.trim();

  String? get _payload {
    final data = _rawPayload;
    return data != null && fitsQrPayload(data) ? data : null;
  }

  String? get _rawPayload {
    switch (_kind) {
      case QrKind.link:
        final raw = _get('url');
        return safeActionUri(raw) != null ? raw : null;
      case QrKind.text:
        return _get('text').isEmpty ? null : _get('text');
      case QrKind.wifi:
        return _get('ssid').isEmpty ? null :
          qrWifi(ssid: _get('ssid'), password: _get('password'), hidden: _hiddenNetwork);
      case QrKind.contact:
        return _get('name').isEmpty ? null :
          qrContact(name: _get('name'), phone: _get('phone'), email: _get('email'));
      case QrKind.email:
        final email = _get('email');
        return RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(email) ? qrEmail(email) : null;
      case QrKind.phone:
        return _get('phone').isEmpty ? null : qrPhone(_get('phone'));
      case QrKind.location:
        final value = _get('location');
        return validCoordinates(value) ? qrLocation(value) : null;
      case QrKind.event:
        return _get('event').isEmpty || !_end.isAfter(_start) ? null :
          qrEvent(title: _get('event'), start: _start, end: _end);
    }
  }

  Widget _field(String key, String label, {String? hint, TextInputType? keyboard,
      int lines = 1, bool obscure = false}) =>
    Padding(padding: const EdgeInsets.only(bottom: 12), child: TextField(
      controller: _fields[key],
      onChanged: (_) => setState(() {}),
      keyboardType: keyboard,
      obscureText: obscure,
      enableSuggestions: !obscure,
      autocorrect: !obscure,
      minLines: lines,
      maxLines: lines,
      decoration: InputDecoration(labelText: label, hintText: hint),
      textCapitalization: TextCapitalization.none,
    ));

  List<Widget> _inputFields() => switch (_kind) {
    QrKind.link => [_field('url', 'Poveznica', hint: 'https://primjer.hr', keyboard: TextInputType.url)],
    QrKind.text => [_field('text', 'Tekst', hint: 'Unesi tekst', lines: 3)],
    QrKind.wifi => [
      _field('ssid', 'Naziv Wi-Fi mreže (SSID)'),
      Padding(padding: const EdgeInsets.only(bottom: 12), child: TextField(
        controller: _fields['password'],
        onChanged: (_) => setState(() {}),
        obscureText: !_wifiPasswordVisible,
        enableSuggestions: false,
        autocorrect: false,
        decoration: InputDecoration(
          labelText: 'Lozinka (prazno za otvorenu mrežu)',
          suffixIcon: IconButton(
            tooltip: _wifiPasswordVisible ? 'Sakrij lozinku' : 'Prikaži lozinku',
            onPressed: () => setState(() => _wifiPasswordVisible = !_wifiPasswordVisible),
            icon: Icon(_wifiPasswordVisible ? Icons.visibility_off_outlined : Icons.visibility_outlined),
          ),
        ),
      )),
      SwitchListTile(
        title: const Text('Skrivena mreža'), value: _hiddenNetwork,
        onChanged: (v) => setState(() => _hiddenNetwork = v),
      ),
    ],
    QrKind.contact => [
      _field('name', 'Ime i prezime'), _field('phone', 'Broj telefona', keyboard: TextInputType.phone),
      _field('email', 'E-mail', keyboard: TextInputType.emailAddress),
    ],
    QrKind.email => [_field('email', 'E-mail adresa', keyboard: TextInputType.emailAddress)],
    QrKind.phone => [_field('phone', 'Broj telefona', keyboard: TextInputType.phone)],
    QrKind.location => [_field('location', 'Koordinate', hint: '45.3271, 14.4422')],
    QrKind.event => [
      _field('event', 'Naziv događaja'),
      ListTile(
        title: const Text('Početak'),
        subtitle: Text(_formatDate(_start)),
        trailing: const Icon(Icons.calendar_month_outlined),
        onTap: () => _pickDate(start: true),
      ),
      ListTile(
        title: const Text('Završetak'),
        subtitle: Text(_formatDate(_end)),
        trailing: const Icon(Icons.calendar_month_outlined),
        onTap: () => _pickDate(start: false),
      ),
    ],
  };

  String _formatDate(DateTime date) => '${date.day}.${date.month}.${date.year}. '
    '${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';

  Future<void> _pickDate({required bool start}) async {
    final existing = start ? _start : _end;
    final day = await showDatePicker(
      context: context, initialDate: existing,
      firstDate: DateTime(2020), lastDate: DateTime(2100),
    );
    if (day == null || !mounted) return;
    final time = await showTimePicker(
      context: context, initialTime: TimeOfDay.fromDateTime(existing),
    );
    if (time == null || !mounted) return;
    final chosen = DateTime(day.year, day.month, day.day, time.hour, time.minute);
    setState(() {
      if (start) {
        _start = chosen;
        if (!_end.isAfter(_start)) _end = _start.add(const Duration(hours: 1));
      } else { _end = chosen; }
    });
  }

  void _message(String message) {
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _copy() async {
    final data = _payload;
    if (data == null) return;
    try {
      await Clipboard.setData(ClipboardData(text: data));
      _message('QR sadržaj je kopiran.');
    } catch (_) {
      _message('Kopiranje nije uspjelo.');
    }
  }

  Future<void> _save() async {
    final data = _payload;
    if (data == null) return;
    try {
      await widget.store.save(data);
      _message('QR kod je spremljen.');
    } catch (_) {
      _message('Spremanje nije uspjelo. Provjeri slobodan prostor na uređaju.');
    }
  }

  Future<void> _shareImage() async {
    if (_payload == null || _busy) return;
    setState(() => _busy = true);
    try {
      final boundary = _qrKey.currentContext?.findRenderObject() as RenderRepaintBoundary?;
      if (boundary == null) throw StateError('QR prikaz nije spreman');
      final image = await boundary.toImage(pixelRatio: 3);
      final data = await image.toByteData(format: ui.ImageByteFormat.png);
      image.dispose();
      if (data == null) throw StateError('PNG nije dostupan');
      final temp = await getTemporaryDirectory();
      final file = File('${temp.path}/qrex-qr.png');
      await file.writeAsBytes(data.buffer.asUint8List(), flush: true);
      if (!mounted) return;
      final box = context.findRenderObject() as RenderBox?;
      await SharePlus.instance.share(ShareParams(
        files: [XFile(file.path, mimeType: 'image/png')],
        sharePositionOrigin: box == null ? null : box.localToGlobal(Offset.zero) & box.size,
      ));
    } catch (_) {
      _message('PNG nije moguće podijeliti.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final payload = _payload;
    return SafeArea(child: ListView(padding: const EdgeInsets.fromLTRB(18, 22, 18, 24), children: [
      Text('Stvori QR kod', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w800)),
      const SizedBox(height: 8),
      const Text('Odaberi vrstu i unesi podatke.', style: TextStyle(color: QrexPalette.muted)),
      const SizedBox(height: 23),
      Container(height: 3, width: double.infinity,
        decoration: const BoxDecoration(gradient: QrexPalette.bluePurple,
          borderRadius: BorderRadius.all(Radius.circular(3)))),
      const SizedBox(height: 18),
      Wrap(spacing: 8, runSpacing: 8, children: [
        for (final type in <QrKind>[
          QrKind.link, QrKind.text, QrKind.wifi, QrKind.contact,
          if (_showAdvanced) ...[
            QrKind.email, QrKind.phone, QrKind.location, QrKind.event,
          ],
        ])
          ChoiceChip(
            label: Text(type.label),
            avatar: Icon(iconForKind(type), size: 18),
            selected: _kind == type,
            onSelected: (_) => setState(() => _kind = type),
          ),
        ActionChip(
          avatar: Icon(_showAdvanced ? Icons.expand_less : Icons.more_horiz, size: 18),
          label: Text(_showAdvanced ? 'Manje' : 'Više'),
          onPressed: () => setState(() {
            _showAdvanced = !_showAdvanced;
            if (!_showAdvanced && !<QrKind>[
              QrKind.link, QrKind.text, QrKind.wifi, QrKind.contact,
            ].contains(_kind)) {
              _kind = QrKind.link;
            }
          }),
        ),
      ]),
      const SizedBox(height: 22),
      ..._inputFields(),
      const SizedBox(height: 12),
      if (_rawPayload != null && _payload == null)
        const Padding(padding: EdgeInsets.only(bottom: 12),
          child: Text('Sadržaj je predugačak za pouzdan QR kod. Skrati podatke.',
            style: TextStyle(color: Colors.orangeAccent))),
      if (_kind == QrKind.wifi)
        const Padding(padding: EdgeInsets.only(bottom: 12),
          child: Text('Wi-Fi QR kod sadrži lozinku. Nemoj ga dijeliti s nepoznatim osobama.',
            style: TextStyle(color: QrexPalette.muted, fontSize: 12))),
      Card(child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 22, horizontal: 12),
        child: Column(children: [
          Text('Pregled koda', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 16),
          LayoutBuilder(builder: (context, limits) {
            final barcodeSide = responsiveQrSide(limits.maxWidth);
            final placeholderSide = responsiveQrSide(
              limits.maxWidth, preferredSide: 250, horizontalInset: 0);
            return payload != null ? RepaintBoundary(
              key: _qrKey,
              child: Container(
                color: Colors.white,
                padding: const EdgeInsets.all(18),
                child: QrImageView(
                  data: payload, version: QrVersions.auto, size: barcodeSide,
                  backgroundColor: Colors.white,
                  errorCorrectionLevel: QrErrorCorrectLevel.M,
                ),
              ),
            ) : SizedBox(
              width: placeholderSide, height: placeholderSide,
              child: const Center(
                child: Text('Unesi valjane podatke za prikaz QR koda.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: QrexPalette.muted)),
              ),
            );
          }),
        ]),
      )),
      const SizedBox(height: 15),
      QrexButton(
        label: _busy ? 'Priprema…' : 'Podijeli QR sliku',
        icon: Icons.ios_share_rounded,
        onPressed: payload == null || _busy ? null : _shareImage,
      ),
      const SizedBox(height: 10),
      Row(children: [
        Expanded(child: OutlinedButton.icon(onPressed: payload == null ? null : _save,
          icon: const Icon(Icons.bookmark_add_outlined), label: const Text('Spremi'))),
        const SizedBox(width: 10),
        Expanded(child: OutlinedButton.icon(onPressed: payload == null ? null : _copy,
          icon: const Icon(Icons.copy_rounded), label: const Text('Kopiraj'))),
      ]),
      const SizedBox(height: 8),
      const Text('Kod se izrađuje na tvom uređaju. Spremljeni QR kodovi ostaju u lokalnoj pohrani.', textAlign: TextAlign.center,
        style: TextStyle(color: QrexPalette.muted, fontSize: 12)),
    ]));
  }
}
