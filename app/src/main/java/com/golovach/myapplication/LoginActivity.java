package com.golovach.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.golovach.myapplication.databinding.ActivityLoginBinding;
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


public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    private FirebaseAuth mAuth;
    private static final int RC_SIGN_IN = 9002; // Define RC_SIGN_IN constant

    private final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            // User is already authenticated, start the MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else {
            binding.signUpBtnGoogle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build();

                    GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(LoginActivity.this, gso);

                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                }
            });
        }

        binding.goToRegisterActivityTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.emailEt.getText().toString();
                String password = binding.passwordEt.getText().toString();
                // Создаем диалоговое окно
                ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
                progressDialog.setMessage("Пожалуйста, подождите...");
                progressDialog.setCancelable(false); // Это предотвратит возможность отмены диалога
                progressDialog.show();
                // Войдите в систему с помощью Firebase Auth
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Вход успешен, обновите UI с информацией о пользователе
                                    Log.d(TAG, "signInWithEmail:success");
                                    progressDialog.dismiss();

                                    // Получаем ссылку на базу данных
                                    DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("users");
                                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                    // Получаем текущую информацию о пользователе
                                    databaseRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            String username = dataSnapshot.child("username").getValue(String.class);

                                            // Обновляем данные в SharedPreferences
                                            SharedPreferences sharedPreferences = getSharedPreferences("MyApp", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putString("username", username);
                                            editor.putString("email", email);
                                            editor.putBoolean("isLogged", true);
                                            editor.apply();

                                            // Обновляем данные в базе данных Firebase
                                            HashMap<String, Object> userInfo = new HashMap<>();
                                            userInfo.put("username", username);
                                            userInfo.put("email", email);
                                            userInfo.put("isLogged", true);
                                            databaseRef.child(userId).updateChildren(userInfo);

                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            finish();
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            // Если произошла ошибка при получении данных
                                            Toast.makeText(LoginActivity.this, "Не удалось получить данные пользователя", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    // Если вход не удался, отобразите сообщение для пользователя.
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(LoginActivity.this, "Аутентификация не удалась.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
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
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Произошла ошибка при входе", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

}