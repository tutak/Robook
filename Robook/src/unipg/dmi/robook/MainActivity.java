package unipg.dmi.robook;

import unipg.dmi.robook.R;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * Activity principale del app. Contiene buttone per avviare il UpdaterService,
 * buttone per avviare la Camera preview, conteggio di numero di likes e number
 * di goal di ogni squadra.
 * 
 * 
 * @author Abdul Rasheed Tutakhail
 * 
 */
public class MainActivity extends AbstractAdkActivity implements
		OnClickListener, OnSharedPreferenceChangeListener {

	String TAG = "MainActivity";
	static SharedPreferences prefs;

	// Variabli che contengono numero di Likes per Apple e Android. I valori
	// vecchie vengono sostituiti con ogni chiamata a Facebook.
	int apple;
	int androidd;
	int oldApple;
	int oldAndroidd;

	int numberOfLikes1 = 0;
	int numberOfLikes2 = 0;

	// Variabili per impostare la telecamera nella Activity
	private Preview mPreview;
	SurfaceView surface;
	SurfaceHolder holder;
	FrameLayout frm;

	// le prime due Variabil contengono numero di Goal di ogni squadra. Gli
	// quattro vengono utilizzati per controllare il movimento backward e foward
	// del Robot quando segna un Goal

	static int goalsA = 0;
	static int goalsB = 0;
	static int charlie = 2;
	static int omega = 2;
	static int stubA = 1;
	static int stubB = 1;

	// Variabili che rappresentano le diverse View della MainActivity
	Button startCamera;
	Button startButton;
	Button stopButton;
	TextView textSquadraA;
	TextView textSquadraB;
	static TextView goalA;
	static TextView goalB;
	TextView fanA;
	TextView fanB;
	static int alpha = 0;
	static int bravo = 0;

	// Viene utilizzato per tenere traccia dello status della rete per verficare
	// che il dispositivo è collegato all'Internet.
	NetworkInfo activeNetwork;
	int i = 0;
	private boolean resumingPending;

	/**
	 * Instanzia i campi dichiarati sopra. Assegna i token di accesso al
	 * SharedPreferences cosi sono accessibile dagli altri elementi del app.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		// Per avviare l'app in Landscape orientation.
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		activeNetwork = conMgr.getActiveNetworkInfo();

		Log.d(TAG, "On created being called");

		startButton = (Button) findViewById(R.id.startService);
		stopButton = (Button) findViewById(R.id.stopService);
		startCamera = (Button) findViewById(R.id.buttonCamera);

		textSquadraA = (TextView) findViewById(R.id.textsquadraA);
		textSquadraB = (TextView) findViewById(R.id.textSquadraB);
		goalA = (TextView) findViewById(R.id.goalA);
		goalB = (TextView) findViewById(R.id.goalB);

		fanA = (TextView) findViewById(R.id.fanA);
		fanB = (TextView) findViewById(R.id.fanB);
		frm = (FrameLayout) findViewById(R.id.frameLayout);

		textSquadraA.setText("0");
		textSquadraB.setText("0");
		fanA.setText("Android non ha nessun tifoso :(");
		fanB.setText("Apple non ha nessun tifoso :(");

		startButton.setOnClickListener(this);
		stopButton.setOnClickListener(this);
		startCamera.setOnClickListener(this);

		prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		this.prefs.registerOnSharedPreferenceChangeListener(this);

		SharedPreferences.Editor editor = prefs.edit();

		// Due token di accesso sono introdotto per evitare eventuale violazione
		// dei linee guida di Facebook
		editor.putString("AKEY", getString(R.string.ACCESS_TOKENA));
		editor.putString("BKEY", getString(R.string.ACCESS_TOKENB));

		editor.putLong("expire", 0);
		editor.commit();

	}

	/**
	 * @param menu
	 *            Sarebbe il Preferences Menu che può essere attivata premendo
	 *            il tasto Menu sul cellulare. Tramite questo menu è possibile
	 *            modificare il tempo (in millisecondi) che UpdaterService
	 *            monitora la pagina Facebook
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// TODO Auto-generated method stub
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		Intent prefs = new Intent(this, prefs.class);

		switch (item.getItemId()) {
		case R.id.delay:
			startActivity(prefs);
			break;
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.buttonCamera:

			stopService(new Intent(this, UpdaterService.class));

			break;
		case R.id.startService:
			if (activeNetwork != null && activeNetwork.isConnected()) {
				startService(new Intent(this, UpdaterService.class));
			} else {
				Toast.makeText(
						this,
						"You seem to be not connected to internet, Check your connection",
						android.widget.Toast.LENGTH_LONG).show();
			}
			break;

		case R.id.stopService:
			mPreview = new Preview(this);
			frm.addView(mPreview);
			mPreview.obtainCamera();

			break;

		default:

		}

	}

	public void onWindowFocusChanged(boolean hasFocus) {

		if (hasFocus && resumingPending) {
			Log.d(TAG, "onWindowFocusChanged -- obtainingCamera");
			mPreview.obtainCamera();
			resumingPending = false;
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		onClick(findViewById(R.id.buttonCamera));
		super.onDestroy();
		Log.d(TAG, "Destroyed was called");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		// Prima che catturiamo la camera, controlliamo se il Keyguard è
		// disattivato
		KeyguardManager myKM = (KeyguardManager) getApplicationContext()
				.getSystemService(Context.KEYGUARD_SERVICE);
		if (myKM.inKeyguardRestrictedInputMode()) {
			resumingPending = true;
			Log.d(TAG, "screenLocked");
		} else {
			// mPreview.obtainCamera();
			resumingPending = false;
			Log.d(TAG, "!screenLocked");
		}
		this.prefs.registerOnSharedPreferenceChangeListener(this);

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		onClick(findViewById(R.id.buttonCamera));

		if (mPreview != null) {
			mPreview.releaseCamera();
		}
		super.onPause();
		this.prefs.unregisterOnSharedPreferenceChangeListener(this);
		Log.d(TAG, "On Pause was called");

	}

	/**
	 * Il listener che viene chiamato quando vengono rilevato dei cambiamenti
	 * sul SharedPreferences. Nel nostro caso ci notifica quando il valore di
	 * Likes di uno dei Post è stato cambiato.
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		// Se il valore di variabili POSTA e POSTB sono valori default
		// (987654321) allora ignora la notifica.
		if (prefs.getInt("POSTA", 0) != 987654321
				&& prefs.getInt("POSTB", 0) != 987654321) {

			Log.d(TAG, "SharedPreferences was called");
			// Se il valore delle variabile POSTA o POSTB sono diversi dal
			// valore di variabili alpha e bravo allora abbiamo un nuovo valore
			// di Likes che bisogna aggiornare sul MainActivity.
			if (alpha != prefs.getInt("POSTA", 123456789)
					|| bravo != prefs.getInt("POSTB", 123456789)) {
				if (alpha != prefs.getInt("POSTA", 123456789)) {
					alpha = prefs.getInt("POSTA", 123456789);
					textSquadraA.setText("Likes per Android: "
							+ Integer.toString(alpha));
					Log.d(TAG, "The value of First Post was updated");

				}

				if (bravo != prefs.getInt("POSTB", 123456789)) {
					bravo = prefs.getInt("POSTB", 123456789);
					textSquadraB.setText("Likes per Apple "
							+ Integer.toString(bravo));
					Log.d(TAG, "The value of Second Post was updated");

				}

				// Visto che i valori di Likes delle squadre sono cambiati,
				// bisogna anche aggiornare il nome dei Fans.
				fanA.setText(prefs.getString("FanA",
						"Android non ha nessun tifoso :("));
				fanB.setText(prefs.getString("FanB",
						"Apple non ha nessun tifoso :("));

			}

		} else {

			Log.d(TAG, "Nothing of importance happend in Main activity");
			Log.d(TAG, "SharedPreferences was called in second clause");
		}
	}

	@Override
	protected void doOnCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doAdkRead(String stringIn) {
		// TODO Auto-generated method stub

	}

	/**
	 * Il metodo valuta i valori dei likes della due squadra e manda il commando
	 * di movimento al Robot chiamando il metodo WriteAdk() della classe
	 * AbstractAdkActivity
	 * 
	 * @param alpha
	 *            Rappresenta il numero di likes della prima squadra
	 * @param bravo
	 *            Rappresenta il numero di likes della seconda squadra
	 * 
	 */
	protected static void callAdk(int alpha, int bravo) {
		if (alpha > bravo) {

			WriteAdk("FORWARD");

		}

		else if (bravo > alpha) {
			WriteAdk("BACKWARD");
		}

		else if (bravo == alpha) {
			WriteAdk("STOP");

		}
	}

	/**
	 * Il metodo viene chiamato dalla class Preview quando un'immagine viene
	 * rilevato dalla Camera. L'obbietivo è quello di aggiornare il numero di
	 * goals e ritornare il Robot alla sua posizione di partenza.
	 * 
	 * @param c
	 */
	public static void arduino(Context c) {
		// Ferma il UpdaterService cosi evitiamo di rispondere ad eventuali
		// cambiamenti in numero di Likes
		c.stopService(new Intent(c, UpdaterService.class));
		// Se alpha è maggiore di bravo significa che il numero di Likes della
		// prima squadra sono maggiori. Per evitare l'esecuzione di questo
		// metodo ogni volta che Preview rileva un'immagine, viene confrontato
		// il valore di Likes di una squadra con charlie * stubA oppure omega *
		// stubB prima di esegiurlo.
		if (alpha > bravo && alpha >= charlie * stubA) {
			goalsA++;
			stubA = goalsA;
			goalA.setText("Goals: " + goalsA);
			// FOWARDB è in effetti BACKWARD con 3 secondi di delay
			WriteAdk("FORWARDB");

		}

		if (alpha < bravo && bravo >= omega * stubB) {

			goalsB++;
			stubB = goalsB;
			goalB.setText("Goals: " + goalsB);
			// FOWARDB è in effetti FORWARD con 3 secondi di delay
			WriteAdk("BACKWARDB");

		}

		// Ora si può ricominciare il UpdaterService, cosi possiamo continuare a
		// monitorare la pagina Facebook.
		c.startService(new Intent(c, UpdaterService.class));
	}
}
