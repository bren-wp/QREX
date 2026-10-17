import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qrex/core/app_store.dart';
import 'package:qrex/core/qr_payload.dart';
import 'package:qrex/screens/create_page.dart';
import 'package:qrex/screens/history_page.dart';
import 'package:qrex/screens/more_page.dart';
import 'package:qrex/screens/result_page.dart';
import 'package:qrex/ui/qrex_theme.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  Future<AppStore> makeStore() => AppStore.load();

  testWidgets('rezultat Wi-Fi koda skriva lozinku dok je korisnik ne otkrije', (tester) async {
    final store = await makeStore();
    final wifi = qrWifi(ssid: 'Moja mreža', password: 'tajna123');
    await tester.pumpWidget(MaterialApp(theme: qrexTheme(false),
      home: ResultPage(store: store, raw: wifi)));
    expect(find.text('Mreža: Moja mreža'), findsOneWidget);
    expect(find.text('Lozinka: ••••••••'), findsOneWidget);
    expect(find.textContaining('tajna123'), findsNothing);
    await tester.tap(find.byTooltip('Prikaži lozinku'));
    await tester.pump();
    expect(find.text('Lozinka: tajna123'), findsOneWidget);
    store.dispose();
  });

  testWidgets('kreator ima jednostavne vrste i dodatne opcije', (tester) async {
    final store = await makeStore();
    await tester.pumpWidget(MaterialApp(theme: qrexTheme(false),
      home: Scaffold(body: CreatePage(store: store))));
    expect(find.text('Stvori QR kod'), findsOneWidget);
    expect(find.text('Više'), findsOneWidget);
    await tester.tap(find.text('Više'));
    await tester.pump();
    expect(find.text('Lokacija'), findsOneWidget);
    expect(find.text('Događaj'), findsOneWidget);
    store.dispose();
  });

  testWidgets('povijest se prikazuje bez pohrane nepouzdanog HTML-a', (tester) async {
    final store = await makeStore();
    await store.record('https://example.com');
    await tester.pumpWidget(MaterialApp(theme: qrexTheme(false),
      home: Scaffold(body: HistoryPage(store: store))));
    expect(find.text('example.com'), findsOneWidget);
    expect(find.text('Povijest'), findsOneWidget);
    store.dispose();
  });

  testWidgets('opcije koriste stvarne radnje bez korisničkog računa', (tester) async {
    final store = await makeStore();
    int? selected;
    await tester.pumpWidget(MaterialApp(theme: qrexTheme(false),
      home: Scaffold(body: MorePage(store: store,
        navigate: (index, {bool gallery = false}) => selected = index))));
    expect(find.text('Skeniraj iz slike'), findsOneWidget);
    await tester.tap(find.text('Stvori QR'));
    expect(selected, 1);
    await tester.scrollUntilVisible(find.text('Spremanje povijesti'), 200,
      scrollable: find.byType(Scrollable).first);
    expect(find.text('Spremanje povijesti'), findsOneWidget);
    store.dispose();
  });

  testWidgets('QR kreator ne preklapa sučelje na uskim ekranima', (tester) async {
    final store = await makeStore();
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
      store.dispose();
    });
    tester.view.devicePixelRatio = 1;
    for (final width in [280.0, 320.0, 360.0, 430.0]) {
      tester.view.physicalSize = Size(width, 640);
      await tester.pumpWidget(MaterialApp(theme: qrexTheme(false),
        home: Scaffold(body: CreatePage(key: ValueKey(width), store: store))));
      await tester.enterText(find.byType(TextField).first, 'https://brendigo.com');
      await tester.pump();
      await tester.scrollUntilVisible(
        find.text('Pregled koda'), 180,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.pump();
      expect(tester.takeException(), isNull, reason: 'Screen width: $width');
    }
  });

  testWidgets('usko sučelje Više nema layout iznimki', (tester) async {
    tester.view.physicalSize = const Size(360, 700);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    final store = await makeStore();
    await tester.pumpWidget(MaterialApp(theme: qrexTheme(false),
      home: Scaffold(body: MorePage(store: store, navigate: (index, {bool gallery = false}) {}))));
    expect(tester.takeException(), isNull);
    store.dispose();
  });
}
