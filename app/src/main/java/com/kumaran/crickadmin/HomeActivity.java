package com.kumaran.crickadmin;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private Spinner teamSpinner;
    private Spinner playerSpinner;
    private Spinner runSpinner;
    private Switch bonusRun;
    private Switch wicket;
    private ProgressBar progressBar;
    private Button update;

    FirebaseFirestore firebaseFireStore = FirebaseFirestore.getInstance();

    private ArrayList<String> teams;
    private ArrayList<String> runs = new ArrayList<>();
    private ArrayList<String> players = new ArrayList<>();
    private ArrayList<String> keylist = new ArrayList<>();
    private ArrayAdapter<String> playeradapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        teamSpinner = findViewById(R.id.team_spinner);
        playerSpinner = findViewById(R.id.player_spinner);
        runSpinner = findViewById(R.id.run_spinner);
        bonusRun = findViewById(R.id.bonus);
        wicket = findViewById(R.id.wicket);
        progressBar = findViewById(R.id.progressbar);
        update = findViewById(R.id.edit_btn);

        runs.add("0");
        runs.add("1");
        runs.add("2");
        runs.add("3");
        runs.add("4");
        runs.add("6");

        playeradapter =new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item,players);

        firebaseFireStore.collection("live_match").document("teams").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                teams = (ArrayList<String>) documentSnapshot.get("name");
                teamSpinner.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item,teams));



            }
        });

        teamSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setPlayers();
                //sets player list in spinner
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        runSpinner.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item,runs));

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(runSpinner.isSelected())
                    Toast.makeText(HomeActivity.this, "run not selected", Toast.LENGTH_SHORT).show();
                firebaseFireStore.collection("live_match").document("team"+(teamSpinner.getSelectedItemPosition()+1)).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Map<String, Object> playerscores = documentSnapshot.getData();
                        Map<String,Object> player = (Map<String, Object>) playerscores.get(keylist.get(playerSpinner.getSelectedItemPosition()));
                        long x = (long) player.get("runs") + Long.valueOf(runSpinner.getSelectedItem().toString());
                        player.put("runs",x);

                        firebaseFireStore.collection("live_match").document("lastrun").update("run",Long.valueOf(runSpinner.getSelectedItem().toString()));

                        if(!bonusRun.isChecked()) {
                            x=(long) player.get("balls")+1;
                            player.put("balls",x);
                        }
                        else{
                            x=(long) player.get("runs")+1;
                            player.put("runs",x);
                        }
                        if(runSpinner.getSelectedItem().toString().contentEquals("4")){
                            x=(long) player.get("fours")+1;
                            player.put("fours",x);
                        }
                        if(runSpinner.getSelectedItem().toString().contentEquals("6")){
                            x=(long) player.get("sixes")+1;
                            player.put("sixes",x);
                        }

                        if(wicket.isChecked()) {
                            player.put("status","OUT");
                            setPlayers();
                        }

                        DecimalFormat formatter = new DecimalFormat("#0.00");
                        Double d= ((Double.valueOf(player.get("runs").toString())) / (Double.valueOf(player.get("balls").toString())) * 100);
                        player.put("strike_rate",formatter.format(d));

                        firebaseFireStore.collection("live_match").document("team"+(teamSpinner.getSelectedItemPosition()+1)).update(playerscores);

                    }
                });

                firebaseFireStore.collection("live_match").document("teams").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        int position = teamSpinner.getSelectedItemPosition();

                        Map<String,Object> teams = documentSnapshot.getData();
                        List<Long> scores = (List<Long>) teams.get("score");
                        scores.set(position,
                                scores.get(position)+Long.valueOf(runSpinner.getSelectedItem().toString()));

                        if (wicket.isChecked()){
                            List<Long> wickets = (List<Long>) teams.get("wickets");
                            wickets.set(position,
                                    wickets.get(position)+1);
                        }

                        if(!bonusRun.isChecked()){
                            List<String> overs = (List<String>) teams.get("overs");
                            int dotPos = overs.get(position).indexOf(".");
                            if(overs.get(position).charAt(dotPos+1)=='5')
                                overs.set(position,
                                        String.valueOf(Integer.valueOf(overs.get(position).substring(0,dotPos))+1)+".0");
                            else
                                overs.set(position,overs.get(position).substring(0,dotPos+1)+String.valueOf(Integer.valueOf(overs.get(position).substring(dotPos+1))+1));
                        }

                        firebaseFireStore.collection("live_match").document("teams").update(teams);
                    }
                });

                bonusRun.setChecked(false);
                wicket.setChecked(false);
            }
        });




    }

    private void setPlayers(){
        progressBar.animate();
        progressBar.setVisibility(View.VISIBLE);

        firebaseFireStore.collection("live_match").document("team"+(teamSpinner.getSelectedItemPosition()+1)).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                players.clear();
                keylist.clear();
                Object[] keys =  documentSnapshot.getData().keySet().toArray();
                for (int i=0;i<keys.length;i++){
                    Map<String,Object> map = (Map) documentSnapshot.get(keys[i].toString());
                    if(!map.get("status").toString().contentEquals("OUT")) {
                        players.add(map.get("name").toString());
                        keylist.add(keys[i].toString());
                    }
                }
                progressBar.setVisibility(View.GONE);

                playerSpinner.setAdapter(playeradapter);
                playeradapter.notifyDataSetChanged();

            }
        });
    }
}