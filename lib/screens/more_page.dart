import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../core/app_store.dart';
import '../ui/qrex_theme.dart';

class MorePage extends StatelessWidget {
  const MorePage({super.key, required this.store, required this.navigate});
  final AppStore store;
  final void Function(int, {bool gallery}) navigate;

  static final Uri _developerUrl = Uri.parse('https://brendigo.com');
  static final Uri _privacyUrl = Uri.parse(
    'https://github.com/bren-wp/QREX/blob/main/docs/PRIVACY.md',
  );

  void _message(BuildContext context, String message) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    }
  }

  Future<void> _openUrl(BuildContext context, Uri url) async {
    try {
      if (!await launchUrl(url, mode: LaunchMode.externalApplication)) {
        if (context.mounted) _message(context, 'Poveznicu nije moguće otvoriti.');
      }
    } catch (_) {
      if (context.mounted) _message(context, 'Na uređaju nema aplikacije za otvaranje poveznice.');
    }
  }

  Future<void> _confirmDelete(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialog) => AlertDialog(
        title: const Text('Izbrisati sve podatke?'),
        content: const Text(
          'Trajno će se ukloniti svi spremljeni kodovi, povijest i postavke '
          'ove aplikacije na uređaju. Radnju nije moguće poništiti.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialog, false),
            child: const Text('Odustani'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialog, true),
            child: const Text('Izbriši sve'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await store.clearAll();
      if (context.mounted) _message(context, 'Podaci i postavke su izbrisani.');
    } catch (_) {
      if (context.mounted) _message(context, 'Brisanje nije u potpunosti uspjelo. Pokušaj ponovno.');
    }
  }

  Future<void> _privacyDialog(BuildContext context) async {
    await showDialog<void>(
      context: context,
      builder: (dialog) => AlertDialog(
        title: const Text('Pravila privatnosti'),
        content: const SizedBox(
          width: 410,
          child: SingleChildScrollView(
            child: Text(
              'QREX je aplikacija za skeniranje i izradu QR kodova. '
              'Za korištenje nije potreban račun.\n\n'
              'Kamera se koristi za čitanje QR kodova. Fotografiju odabireš '
              'samostalno iz galerije; aplikacija je obrađuje lokalno.\n\n'
              'Skeniranja mogu ostati u lokalnoj povijesti uređaja ako '
              'je uključiš. Wi-Fi kodovi ne spremaju se automatski. '
              'Ručno spremljeni Wi-Fi kod može sadržavati lozinku. '
              'Lokalna pohrana postavki nije šifrirani trezor.\n\n'
              'QREX nema vlastiti račun, sustav oglasa, analitiku ni '
              'poslužitelj za prikupljanje QR sadržaja. Sadržaj se ne '
              'šalje razvojnom timu.\n\n'
              'Kada odabereš otvaranje poveznice, poziv, e-mail, kartu '
              'ili dijeljenje, podatke može obraditi druga aplikacija ili '
              'internetska usluga prema svojim pravilima.\n\n'
              'Povijest možeš isključiti. Svoje lokalne podatke možeš '
              'izbrisati odabirom „Izbriši sve podatke”. Pohranjene '
              'postavke mogu biti obuhvaćene sigurnosnom kopijom uređaja, '
              'ovisno o sustavu i korisničkim postavkama.\n\n'
              'Razvoj: Brendigo · brendigo.com. Za upite o privatnosti '
              'koristi kontaktni kanal naveden na web-stranici Brendigo.',
              style: TextStyle(height: 1.5),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialog),
            child: const Text('Zatvori'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(dialog);
              _openUrl(context, _privacyUrl);
            },
            child: const Text('Javna verzija'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: store,
      builder: (context, _) => SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(18, 22, 18, 26),
          children: [
            Text('Više', style: Theme.of(context).textTheme.headlineMedium
                ?.copyWith(fontWeight: FontWeight.w800)),
            const SizedBox(height: 8),
            const Text('Sve na jednom mjestu.',
              style: TextStyle(color: QrexPalette.muted)),
            const SizedBox(height: 22),
            Card(child: Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 19),
              decoration: BoxDecoration(
                gradient: QrexPalette.bluePurple,
                borderRadius: BorderRadius.circular(18),
              ),
              child: const Row(children: [
                QrexMark(size: 42),
                SizedBox(width: 16),
                Expanded(child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Skeniraj. Stvori. Dijeli.',
                      style: TextStyle(color: Colors.white,
                        fontWeight: FontWeight.w800, fontSize: 17)),
                    SizedBox(height: 5),
                    Text('Sve što trebaš za QR, bez prijave.',
                      style: TextStyle(color: Colors.white, fontSize: 12)),
                  ],
                )),
              ]),
            )),
            const SizedBox(height: 14),
            GridView.count(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              crossAxisCount: 2,
              childAspectRatio: 1.6,
              mainAxisSpacing: 12,
              crossAxisSpacing: 12,
              children: [
                _action(context, Icons.qr_code_scanner_rounded, 'Skeniraj',
                  () => navigate(0)),
                _action(context, Icons.add_circle_outline_rounded, 'Stvori QR',
                  () => navigate(1)),
                _action(context, Icons.history, 'Povijest', () => navigate(2)),
                _action(context, Icons.image_outlined, 'Skeniraj iz slike',
                  () => navigate(0, gallery: true)),
              ],
            ),
            const SizedBox(height: 26),
            Text('Postavke', style: Theme.of(context).textTheme.titleLarge
                ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Card(child: Column(children: [
              SwitchListTile(
                secondary: const Icon(Icons.light_mode_outlined,
                  color: QrexPalette.primary),
                title: const Text('Svijetli izgled'),
                value: store.light,
                onChanged: (value) => _updateSetting(
                  context, () => store.setLight(value)),
              ),
              const Divider(height: 1),
              SwitchListTile(
                secondary: const Icon(Icons.history_toggle_off,
                  color: QrexPalette.primary),
                title: const Text('Spremanje povijesti'),
                subtitle: const Text('Skeniranja ostaju na uređaju.'),
                value: store.historyEnabled,
                onChanged: (value) => _updateSetting(
                  context, () => store.setHistoryEnabled(value)),
              ),
              const Divider(height: 1),
              ListTile(
                leading: const Icon(Icons.delete_outline,
                  color: Colors.redAccent),
                title: const Text('Izbriši sve podatke'),
                onTap: () => _confirmDelete(context),
              ),
            ])),
            const SizedBox(height: 20),
            Text('Privatnost', style: Theme.of(context).textTheme.titleLarge
                ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Card(child: Column(children: [
              const Padding(
                padding: EdgeInsets.fromLTRB(18, 16, 18, 8),
                child: Text(
                  'Bez prijave, oglasa i analitičkog praćenja. '
                  'QR kodovi obrađuju se na uređaju. '
                  'Vanjske radnje mogu koristiti druge aplikacije.',
                  style: TextStyle(height: 1.5),
                ),
              ),
              ListTile(
                leading: const Icon(Icons.shield_outlined,
                  color: QrexPalette.primary),
                title: const Text('Pročitaj pravila privatnosti'),
                subtitle: const Text('Dostupno i bez interneta'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => _privacyDialog(context),
              ),
            ])),
            const SizedBox(height: 22),
            Text('O aplikaciji', style: Theme.of(context).textTheme.titleLarge
                ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Card(child: Column(children: [
              ListTile(
                leading: const QrexMark(size: 40),
                title: const Text('QREX'),
                subtitle: const Text('Verzija 0.1.3 · Android i iOS'),
              ),
              const Divider(height: 1),
              ListTile(
                leading: const Icon(Icons.code_rounded,
                  color: QrexPalette.cyan),
                title: const Text('Razvio Brendigo'),
                subtitle: const Text('brendigo.com'),
                trailing: const Icon(Icons.open_in_new, size: 19),
                onTap: () => _openUrl(context, _developerUrl),
              ),
            ])),
            const SizedBox(height: 20),
            const Center(child: Text('QREX · Skeniraj. Stvori. Dijeli.',
              style: TextStyle(color: QrexPalette.muted, fontSize: 12))),
          ],
        ),
      ),
    );
  }

  Future<void> _updateSetting(
    BuildContext context, Future<void> Function() callback,
  ) async {
    try {
      await callback();
    } catch (_) {
      if (context.mounted) _message(context, 'Postavka nije spremljena. Pokušaj ponovno.');
    }
  }

  Widget _action(BuildContext context, IconData icon,
      String label, VoidCallback onTap) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 29, color: QrexPalette.primary),
            const SizedBox(height: 10),
            Text(label, textAlign: TextAlign.center,
              style: const TextStyle(fontWeight: FontWeight.w600)),
          ],
        ),
      ),
    );
  }
}
