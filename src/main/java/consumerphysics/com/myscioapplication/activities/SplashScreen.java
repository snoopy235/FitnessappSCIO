package consumerphysics.com.myscioapplication.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import consumerphysics.com.myscioapplication.R;

/**
 * Created by student on 14.06.16.
 */
public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        initGui();

        Thread mythread = new Thread(){ //Thread for displaying splashscreen
            @Override
            public void run() {
                try {
                    sleep(3000); //Thread sleep for 3 second
                    Intent startLoginScreen = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(startLoginScreen); //start LoginScreen
                    finish();//finish splash activity
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        };

        mythread.start(); //startpoint of secondary thread


        //space for loading database

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.finish();
    }

    private void initGui(){
       setContentView(R.layout.splash_screen);

    }
}
