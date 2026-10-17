import 'package:flutter/material.dart';

import '../core/app_store.dart';
import '../ui/qrex_theme.dart';
import 'create_page.dart';
import 'history_page.dart';
import 'more_page.dart';
import 'scanner_page.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({super.key, required this.store});
  final AppStore store;
  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _page = 0;
  int _scannerRevision = 0;
  bool _gallery = false;

  void _select(int index, {bool gallery = false}) {
    setState(() {
      if (index == 0 && (gallery || _page != 0)) _scannerRevision++;
      _page = index;
      _gallery = gallery;
    });
  }

  @override
  Widget build(BuildContext context) {
    final light = Theme.of(context).brightness == Brightness.light;
    final Widget content = switch (_page) {
      0 => ScannerPage(key: ValueKey('scanner-$_scannerRevision'),
        store: widget.store, openGalleryInitially: _gallery),
      1 => CreatePage(store: widget.store),
      2 => HistoryPage(store: widget.store),
      _ => MorePage(store: widget.store, navigate: _select),
    };
    const destinations = <({String label, IconData icon, IconData selected})>[
      (label: 'Skeniraj', icon: Icons.qr_code_scanner_outlined, selected: Icons.qr_code_scanner),
      (label: 'Stvori', icon: Icons.add_circle_outline, selected: Icons.add_circle),
      (label: 'Povijest', icon: Icons.history_outlined, selected: Icons.history),
      (label: 'Više', icon: Icons.grid_view_outlined, selected: Icons.grid_view_rounded),
    ];
    return Scaffold(
      backgroundColor: light ? const Color(0xFFF3F6FF) : QrexPalette.base,
      body: content,
      bottomNavigationBar: DecoratedBox(
        decoration: BoxDecoration(
          color: light ? Colors.white : const Color(0xFF071225),
          border: Border(top: BorderSide(color: light ? Colors.black12 : QrexPalette.border)),
        ),
        child: SafeArea(top: false, child: Padding(
          padding: const EdgeInsets.fromLTRB(8, 5, 8, 4),
          child: Row(children: [
            for (var i = 0; i < destinations.length; i++)
              Expanded(child: Semantics(
                selected: _page == i, button: true, label: destinations[i].label,
                child: InkWell(
                  key: ValueKey('nav-$i'),
                  borderRadius: BorderRadius.circular(14),
                  onTap: () => _select(i),
                  child: Column(mainAxisSize: MainAxisSize.min, children: [
                    const SizedBox(height: 5),
                    Icon(_page == i ? destinations[i].selected : destinations[i].icon,
                      color: _page == i ? QrexPalette.cyan :
                        (light ? const Color(0xFF56677B) : QrexPalette.muted), size: 25),
                    const SizedBox(height: 5),
                    Text(destinations[i].label,
                      maxLines: 1,
                      softWrap: false,
                      overflow: TextOverflow.ellipsis,
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 11,
                        fontWeight: _page == i ? FontWeight.w700 : FontWeight.w500,
                        color: _page == i ? QrexPalette.cyan :
                          (light ? const Color(0xFF56677B) : QrexPalette.muted))),
                    const SizedBox(height: 6),
                    AnimatedContainer(duration: const Duration(milliseconds: 180),
                      width: _page == i ? 33 : 0, height: 2.5,
                      decoration: BoxDecoration(gradient: QrexPalette.bluePurple,
                        borderRadius: BorderRadius.circular(4))),
                  ]),
                ),
              )),
          ]),
        )),
      ),
    );
  }
}
