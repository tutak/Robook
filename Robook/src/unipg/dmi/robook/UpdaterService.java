package unipg.dmi.robook;

import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.Util;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * UpdaterService monitora la pagina Facebook per prendere i valori aggiornati
 * di Likes di ogni squadra e lo passa al MainActivity
 * 
 * @author Abdul Rasheed Tutakhail
 * 
 */
public class UpdaterService extends Service {

	private static final String TAG = "UpdaterService";
	private boolean Flag = false;
	private Updater updater;
	boolean flagA = false;
	boolean flagB = false;
	SharedPreferences prefs;
	SharedPreferences.Editor editor;
	String ret = "";
	int likes;
	Long sleepy = 2000L;
	static int apple = 0;
	static int androidd = 0;
	static int appleOld = 0;
	static int androiddOld = 0;
	static int contatore = 0;
	static int var1 = 0;
	static int var2 = 0;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.updater = new Updater();
		prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		editor = prefs.edit();
		editor.putInt("POSTA", 987654321);
		editor.putInt("POSTB", 987654321);
		editor.putString("FanA", "");
		editor.putString("FanB", "");
		editor.commit();

		// editor.apply();
		Log.d(TAG, "Created");

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.Flag = true;
		if (!this.updater.isAlive()) {
			this.updater.start();
			Log.d(TAG, "Started");

		}

		return START_STICKY;

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.Flag = false;
		this.updater.interrupt();
		this.updater = null;
		Log.d(TAG, "onDestroyed");
	}

	private class Updater extends Thread {
		public Updater() {
			super("UpdaterService-Updater");
		}

		/**
		 * Quando il Flag è True, viene eseguito questo metodo. In effetti vieni
		 * fatto 4 chiamate GET. Nelle Prime due chiamate riceviamo il numero di
		 * Likes. Nelle ultime due invece riceviamo il nome del ultima persona
		 * che ha messo Mi Piace sul post.
		 */
		@Override
		public void run() {

			UpdaterService updaterService = UpdaterService.this;
			while (updaterService.Flag) {
				Log.d(TAG, "Updater running");

				try {

					JSONObject post1 = new JSONObject();

					post1.put("method", "GET");
					post1.put("relative_url",
							"259240967554711_559947950817343/?fields=likes.limit(1).summary(true)&"
									+ prefs.getString("AKEY", ""));

					JSONObject post2 = new JSONObject();

					post2.put("method", "GET");
					post2.put("relative_url",
							"259240967554711_559948050817333/?fields=likes.limit(1).summary(true)&"
									+ prefs.getString("BKEY", ""));

					JSONObject post3 = new JSONObject();
					post3.put("method", "GET");
					post3.put("relative_url",
							"259240967554711_559947950817343/?fields=likes{name}&"
									+ prefs.getString("AKEY", ""));

					JSONObject post4 = new JSONObject();
					post4.put("method", "GET");
					post4.put("relative_url",
							"259240967554711_559948050817333/?fields=likes{name}&"
									+ prefs.getString("BKEY", ""));

					final JSONArray batch_array = new JSONArray();

					batch_array.put(post1);
					batch_array.put(post2);
					batch_array.put(post3);
					batch_array.put(post4);

					final Bundle args = new Bundle();
					args.putString("access_token", prefs.getString("BKEY", ""));
					args.putString("batch", batch_array.toString());

					String url = "https://graph.facebook.com";

					ret = Util.openUrl(url, "POST", args);
					JSONArray arr = new JSONArray(ret);

					for (int i = 0; i < arr.length(); i++) {
						// Qui viene gestito la risposta JSON appartenente ai
						// numero di Likes
						if (i == 0 || i == 1) {

							JSONObject obj = arr.getJSONObject(i);
							String str = obj.getString("body");
							// Vengono tolti i Backslash ridondanti nella
							// risposta JSON dal Facebook
							str.replaceAll("\\\\", "");
							JSONObject obj1 = new JSONObject(str);
							if (obj1.has("likes")) {
								JSONObject obj2 = obj1.getJSONObject("likes");
								JSONObject obj3 = obj2.getJSONObject("summary");
								likes = obj3.getInt("total_count");
							}
							// Se non abbiamo il campo likes nella risposta
							// JSON, allora significa che quel post non ha
							// nessun Like.
							else {

								likes = 0;
							}
							if (i == 0) {
								Log.d(TAG, "Earlier Value of likes A was"
										+ prefs.getInt("POSTA", 0));
								editor.putInt("POSTA", likes);
								Log.d(TAG, "New Value entered in first post is"
										+ likes);

								androidd = likes;

							} else if (i == 1) {
								Log.d(TAG, "Earlier Value of likes B was"
										+ prefs.getInt("POSTB", 0));

								editor.putInt("POSTB", likes);
								Log.d(TAG,
										"new Value entered in second post is"
												+ likes);
								apple = likes;

							}
							// var1 e var2 vengono utilizzati per continuare il
							// gioco basandosi sui valori nuovi di Likes delle
							// due squadre.
							var1 = androidd - androiddOld;
							var2 = apple - appleOld;

							if (var1 < 0) {
								var1 = 0;
							}

							if (var2 < 0) {
								var2 = 0;
							}

							MainActivity.callAdk(var1, var2);

							androiddOld = androidd;
							appleOld = apple;

						}

						// Stiamo considerando la risposta JSON che contiene i
						// nomi delle ultime persone che hanno messo Mi Piace
						// sui post appartenenti alle squadre.
						else {

							JSONObject obj = arr.getJSONObject(i);
							String str1 = obj.getString("body");
							str1.replaceAll("\\\\", "");
							JSONObject obj1 = new JSONObject(str1);

							if (obj1.has("likes")) {
								JSONObject obj2 = obj1.getJSONObject("likes");
								JSONArray arr1 = obj2.getJSONArray("data");
								JSONObject obj3 = arr1.getJSONObject(0);

								String fan = obj3.getString("name");
								Log.d("Last Fan is", fan);

								if (i == 2) {

									editor.putString("FanA", fan
											+ " tifa squadra Android :)");
								} else {

									editor.putString("FanB", fan
											+ " tifa squadra Apple :)");
								}
							} else {

								if (i == 2) {

									editor.putString("FanA",
											"Android non ha nessun tifoso :(");
								} else {

									editor.putString("FanB",
											"Apple non ha nessun tifoso :(");
								}

								Log.d("Fan", "Nobody supports Team yet :(");
							}

						}
					}

					editor.commit();
					editor.apply();

				}

				catch (JSONException e) {
					updaterService.Flag = false;

					e.printStackTrace();
					Log.e("TAG", e.getMessage());

				} catch (MalformedURLException e) {
					Log.d("reto1", "" + e);
					updaterService.Flag = false;

					e.printStackTrace();
				} catch (IOException e) {
					Log.d("reto2", "" + e);
					updaterService.Flag = false;
					e.printStackTrace();
				}

				Log.d(TAG, "Updater ran");
				try {
					// Numero di millisecondi che l'app monitora la pagina. Il
					// valore di questo delay può essere cambiato direttamente
					// dalla GUI, premendo il tasto Menu.
					String sleepString = prefs.getString("delay", "2000");
					sleepy = Long.parseLong(sleepString);
					Thread.sleep(sleepy);

					editor.putInt("POSTA", 987654321);
					editor.putInt("POSTB", 987654321);
					editor.putString("FanA", "");
					editor.putString("FanB", "");
					editor.commit();

				} catch (InterruptedException e) {
					Log.d("Value of Delay is: ",
							prefs.getString("delay", "2000"));
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}
}
