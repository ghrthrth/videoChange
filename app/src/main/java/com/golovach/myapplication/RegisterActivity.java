package com.golovach.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.golovach.myapplication.databinding.ActivityRegisterBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;


public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    private FirebaseAuth mAuth;
    private static final int RC_SIGN_IN = 9001; // Define RC_SIGN_IN constant
    private final String TAG = "ferfgreg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();

        // Check if the user is already signed in
            binding.signUpBtnGoogle.setOnClickListener(v -> {
                // Create a GoogleSignInOptions object with the desired scopes
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

                // Build a GoogleSignInClient with the options specified by gso
                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(RegisterActivity.this, gso);

                // Start the sign-in intent
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        binding.backBtn.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));

        binding.signUpBtn.setOnClickListener(v -> {
            // Получаем данные из полей ввода здесь, когда пользователь нажимает кнопку регистрации
            String password = binding.passwordEt.getText().toString();
            String username = binding.usernameEt.getText().toString();
            String email = binding.emailEt.getText().toString();

            // Проверяем ввод пользователя
            if (username.isEmpty() || username.length() < 8) {
                Toast.makeText(RegisterActivity.this, "Логин должен содержать минимум 8 символов", Toast.LENGTH_SHORT).show();
                return;
            }

            if (email.isEmpty() || !email.contains("@") || email.length() > 50) {
                Toast.makeText(RegisterActivity.this, "Почта должна содержать знак @ и быть не более 50 символов", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty() || password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
                return;
            }

            // Создаем диалоговое окно
            ProgressDialog progressDialog = new ProgressDialog(RegisterActivity.this);
            progressDialog.setMessage("Пожалуйста, подождите...");
            progressDialog.setCancelable(false); // Это предотвратит возможность отмены диалога
            progressDialog.show();

            // Создайте нового пользователя с помощью Firebase Auth
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()) {
                                // Пользователь успешно зарегистрирован, теперь добавьте дополнительные данные в базу данных Firebase
                                DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("users");
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                HashMap<String, Object> userInfo = new HashMap<>();
                                userInfo.put("username", username);
                                userInfo.put("email", email);
                                userInfo.put("isLogged", true);

                                SharedPreferences sharedPreferences = getSharedPreferences("MyApp", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("username", username);
                                editor.putString("email", email);
                                editor.putBoolean("isLogged", true);
                                editor.apply();


                                databaseRef.child(userId).setValue(userInfo)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    // Данные успешно записаны в базу данных
                                                    Toast.makeText(RegisterActivity.this, "Пользователь успешно зарегистрирован", Toast.LENGTH_SHORT).show();
                                                    // Скрываем диалоговое окно
                                                    progressDialog.dismiss();
                                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                } else {
                                                    // Произошла ошибка при записи в базу данных
                                                    Toast.makeText(RegisterActivity.this, "Не удалось зарегистрировать пользователя", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            } else {
                                // Произошла ошибка при регистрации пользователя
                                Toast.makeText(RegisterActivity.this, "Не удалось зарегистрировать пользователя", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, handle the error
                Log.w(TAG, "Google sign in failed", e);
                // TODO: Handle sign-in failure
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Log.d(TAG, "signInWithCredential:success" + user.getEmail());
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                Toast.makeText(RegisterActivity.this, "Вы уже авторизованы!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(RegisterActivity.this, "Произошла ошибка!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Произошла ошибка!", Toast.LENGTH_SHORT).show();
                        }
                    }});
    }
}