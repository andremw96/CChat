package com.example.andre.cchat.view.register;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.andre.cchat.R;
import com.example.andre.cchat.view.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import de.frank_durr.ecdh_curve25519.ECDHCurve25519;

public class RegisterActivity extends AppCompatActivity {

    public static final String TAG = ECDHCurve25519.class.getName();

    static {
        try {
            System.loadLibrary("ecdhcurve25519");
            Log.i(TAG, "Loaded ecdhcurve25519 library.");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private FirebaseAuth mAuth;
    private DatabaseReference storeUserDefaultDataReference; // mengambil route database di firebase

    private ProgressDialog loadingBar;

    private EditText registerUserName;
    private EditText registerUserEmail;
    private EditText registerUserPassword;

    String yourPublicKeyStr;
    String encryptedPrivateKey;
    String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        Toolbar mToolbar = (Toolbar) findViewById(R.id.register_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Sign Up");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        registerUserName = (EditText) findViewById(R.id.register_name);
        registerUserEmail = (EditText) findViewById(R.id.register_email);
        registerUserPassword = (EditText) findViewById(R.id.register_password);
        Button buatAkunButton = (Button) findViewById(R.id.buat_akun_button);
        loadingBar = new ProgressDialog(this);


        buatAkunButton.setOnClickListener(view -> {
            // memunuclkan loadingbar
            loadingBar.setTitle("Akun baru sedang dibuat");
            loadingBar.setMessage("Silahkan Tunggu, ketika akun sedang dibuat");
            loadingBar.show();

            final String name = registerUserName.getText().toString();
            email = registerUserEmail.getText().toString();
            final String pwd = registerUserPassword.getText().toString();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(RegisterActivity.this, "Masukkan nama anda.", Toast.LENGTH_LONG).show();
                return;
            }

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(RegisterActivity.this, "Masukkan email anda.", Toast.LENGTH_LONG).show();
                return;
            }

            if (TextUtils.isEmpty(pwd)) {
                Toast.makeText(RegisterActivity.this, "Masukkan password anda.", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                generateUserKey();
                final String publicKey = yourPublicKeyStr;
                final String encryptedPrivKey = encryptedPrivateKey;
                Log.d("public key", publicKey);
                Log.d("encrypted priv key", encryptedPrivKey);

                daftarkanAkun(name, email, pwd, publicKey, encryptedPrivKey);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // dissmiss loading bar
            loadingBar.dismiss();
        });
    }

    private void generateUserKey() throws Exception {
        //start
        long lStartTime = System.nanoTime();

        //task
        SecureRandom random = new SecureRandom();
        byte[] yourPrivateKey = ECDHCurve25519.generate_secret_key(random);
        String yourPrivateKeyStr = bytesToHex(yourPrivateKey);

        // Create Alice's public key.
        byte[] yourPublicKey = ECDHCurve25519.generate_public_key(yourPrivateKey);
        yourPublicKeyStr = bytesToHex(yourPublicKey);

        Log.d("emailnya", email);

        // enkripsi privatekey dgn AES
        encryptedPrivateKey = encrypt(yourPrivateKeyStr, email);

        Log.d("kunci publiknya", yourPublicKeyStr);
        Log.d("kunci prviat sbm enkrip", yourPrivateKeyStr);
        Log.d("kunci prviat sdh enkrip", encryptedPrivateKey);

        //registerUserPrivateKey.setText(encrypted_private_key);
        //end
        long lEndTime = System.nanoTime();

        //time elapsed
        long output = lEndTime - lStartTime;

        //              long durationInMs = TimeUnit.NANOSECONDS.toMillis(output);

        System.out.println("Waktu Menghasilkan Kunci dalam milliseconds: " + output / 1000000);
    }


    private void daftarkanAkun(final String name, String email, String pwd, final String publicKey, final String encryptedPrivKey) throws Exception {
        // validasi mengecek apakaah field kosong atau tidak
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(RegisterActivity.this, "Masukkan nama anda.", Toast.LENGTH_LONG).show();
        }

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(RegisterActivity.this, "Masukkan email anda.", Toast.LENGTH_LONG).show();
        }

        if (TextUtils.isEmpty(pwd)) {
            Toast.makeText(RegisterActivity.this, "Masukkan password anda.", Toast.LENGTH_LONG).show();
        }

        if (TextUtils.isEmpty(publicKey)) {
            Toast.makeText(RegisterActivity.this, "Ada Kesalahan, Mohon Coba Lagi.... ", Toast.LENGTH_LONG).show();
        }

        if (TextUtils.isEmpty(encryptedPrivKey)) {
            Toast.makeText(RegisterActivity.this, "Ada Kesalahan, Mohon Coba Lagi Lagi.... ", Toast.LENGTH_LONG).show();
        } else {

            // script untuk memasukkan data  email dan password ke firebase
            mAuth.createUserWithEmailAndPassword(email, pwd).addOnCompleteListener(task -> {
                // jika berhasil menyimpan maka, user akan dialihkan ke main
                if (task.isSuccessful()) {
                    String deviceToken = FirebaseInstanceId.getInstance().getToken();

                    String currentUserId = mAuth.getCurrentUser().getUid();
                    // create reference and store the reference inside variable, reference to firebase database
                    storeUserDefaultDataReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);

                    storeUserDefaultDataReference.child("user_name").setValue(name);
                    storeUserDefaultDataReference.child("user_name_lowercase").setValue(name.toLowerCase());
                    storeUserDefaultDataReference.child("user_public_key").setValue(publicKey);
                    storeUserDefaultDataReference.child("user_private_key").setValue(encryptedPrivKey);
                    storeUserDefaultDataReference.child("user_status").setValue("Hello World, I am using CChat");
                    storeUserDefaultDataReference.child("user_image").setValue("default_profile");
                    storeUserDefaultDataReference.child("device_token").setValue(deviceToken);
                    storeUserDefaultDataReference.child("user_thumb_image").setValue("default_image")
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                                    // validasi user tidak kembali setelah register activity
                                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(mainIntent);
                                    finish();
                                }
                            });
                } else {
                    Toast.makeText(RegisterActivity.this, "Ada kesalahan, silahkan coba lagi", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // email sebagai kunci enkripsi
    private String encrypt(String Data, String email) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        SecretKeySpec key = generateKey(email, salt);
        String AES = "AES/CBC/PKCS5Padding";
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.ENCRYPT_MODE, key);
        AlgorithmParameters params = c.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        byte[] encryptedText = c.doFinal(Data.getBytes(StandardCharsets.UTF_8));

        // concatenate salt + iv + ciphertext
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(salt);
        outputStream.write(iv);
        outputStream.write(encryptedText);

        //  byte[] encVal = c.doFinal(Data.getBytes());
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }


    private SecretKeySpec generateKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] key = f.generateSecret(spec).getEncoded();

        return new SecretKeySpec(key, "AES");
    }
}
