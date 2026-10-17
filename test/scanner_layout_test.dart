import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qrex/ui/qrex_theme.dart';

void main() {
  test('QR preview stays within usable width without changing normal sizing', () {
    expect(responsiveQrSide(420), 214);
    expect(responsiveQrSide(250), 214);
    expect(responsiveQrSide(210), 174);
    expect(responsiveQrSide(100), 64);
    expect(responsiveQrSide(-10), 0);
    expect(responsiveQrSide(double.infinity), 0);
    expect(responsiveQrSide(230, preferredSide: 220, horizontalInset: 84), 146);
  });


  testWidgets('tipka može biti onemogućena bez izvršavanja radnje', (tester) async {
    var clicks = 0;
    await tester.pumpWidget(MaterialApp(theme: qrexTheme(false),
      home: Scaffold(body: Column(children: [
        QrexButton(label: 'Nije spremno', onPressed: null, icon: Icons.share),
        QrexButton(label: 'Spremno', onPressed: () => clicks++, icon: Icons.check),
      ]))));
    await tester.tap(find.text('Nije spremno'));
    await tester.tap(find.text('Spremno'));
    expect(clicks, 1);
  });
}
