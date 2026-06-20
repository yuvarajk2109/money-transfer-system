import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Navbar } from './shared/navbar/navbar';
import { ModalService } from './core/services/modal.service';
import { AuthService } from './core/services/auth.service';
import { ConfirmDialog } from './shared/confirm-dialog/confirm-dialog';
import FontFaceObserver from 'fontfaceobserver';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar, ConfirmDialog],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('money-transfer-ui');

  showLogoutModal = false;

  constructor(
    private modal: ModalService,
    private auth: AuthService
  ) {
    this.modal.logoutModal$.subscribe(value => {
      this.showLogoutModal = value;
    });

    this.loadFonts();
  }

  loadFonts() {
    if (document.documentElement.classList.contains('webfont-loaded')) {
      return;
    }

    var fidelitySansBold = new FontFaceObserver('Fidelity Sans Bold'),
      fidelitySansBoldItalic = new FontFaceObserver('Fidelity Sans Bold Italic'),
      fidelityExtraBold = new FontFaceObserver('Fidelity Sans Extra Bold'),
      fidelitySansItalic = new FontFaceObserver('Fidelity Sans Italic'),
      fidelitySansLight = new FontFaceObserver('Fidelity Sans Light'),
      fidelitySansLightItalic = new FontFaceObserver('Fidelity Sans Light Italic'),
      fidelitySansRegular = new FontFaceObserver('Fidelity Sans Regular'),
      fidelitySansUltraLight = new FontFaceObserver('Fidelity Sans Ultra Light'),
      fidelitySansCondensedMedium = new FontFaceObserver('Fidelity Sans Condensed Medium'),
      fidelitySansDemibold = new FontFaceObserver('Fidelity Sans Demibold');

    Promise.all([
      fidelitySansBold.load(),
      fidelitySansBoldItalic.load(),
      fidelityExtraBold.load(),
      fidelitySansItalic.load(),
      fidelitySansLight.load(),
      fidelitySansLightItalic.load(),
      fidelitySansRegular.load(),
      fidelitySansUltraLight.load(),
      fidelitySansCondensedMedium.load(),
      fidelitySansDemibold.load()
    ]).then(
      function () {
        document.documentElement.classList.add('webfont-loaded');
        // Optional: set a cookie so the SSI directive works on reload
        document.cookie = "webfont-loaded=true; path=/; max-age=31536000";
      },
      function () {
        console.info('Web fonts could not be loaded in time. Falling back to system fonts.');
      }
    );
  }
  confirmLogout() {
    this.modal.closeLogout();
    this.auth.logout();
  }

  closeLogout() {
    this.modal.closeLogout();
  }

}
