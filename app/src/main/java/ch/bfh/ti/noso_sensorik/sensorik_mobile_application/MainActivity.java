package ch.bfh.ti.noso_sensorik.sensorik_mobile_application;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    protected static final String TAG = "MainActivity";
    public static final String JOB = "com.example.myfirstapp.MESSAGE.JOB";
    public static final String DEPARTMENT = "com.example.myfirstapp.MESSAGE.DEPARTMENT";

    private static final int REQUEST_CODE_PERMISSIONS = 100;

    private String department;
    private String job;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prepareFormSpinner();
        prepareScrubbottleSelection();
        checkPermissions();
    }

    /**
     * Prepares the two spinners for department selection and job selection
     */
    protected void prepareFormSpinner(){
        Spinner spinner_dep = (Spinner) findViewById(R.id.spinner_department);
        ArrayAdapter<CharSequence> adapter_dep = ArrayAdapter.createFromResource(this, R.array.departments_array, android.R.layout.simple_spinner_item);
        adapter_dep.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_dep.setAdapter(adapter_dep);
        spinner_dep.setOnItemSelectedListener(this);

        Spinner spinner_job = (Spinner) findViewById(R.id.spinner_job);
        ArrayAdapter<CharSequence> adapter_job= ArrayAdapter.createFromResource(this, R.array.jobs_array, android.R.layout.simple_spinner_item);
        adapter_job.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_job.setAdapter(adapter_job);
        spinner_job.setOnItemSelectedListener(this);

        // set job/dep string from default selection
        job = adapter_job.getItem(0).toString();
        department = adapter_dep.getItem(0).toString();
    }

    // HACK IDEA: display all found scrubbottle sensor (Major == 3) in range -
    // make user select the one he has based on the name/id (like 'ntZ2co')
    // and save the name in the application and keep tabs on the sensor during Tracking Activity
    // (maybe make a filter for it?) and analyse its acceleratings
    protected void prepareScrubbottleSelection(){

    }

    //Since Android Marshmallow starting a Bluetooth Low Energy scan requires permission from location group.
    private void checkPermissions() {
        int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermissionResult) {
            //Permission not granted so we ask for it. Results are handled in onRequestPermissionsResult() callback.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (REQUEST_CODE_PERMISSIONS == requestCode) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            }
        } else {
            disableButtons();
            Toast.makeText(this, "Location permissions are mandatory to use BLE features on Android 6.0 or higher", Toast.LENGTH_LONG).show();
        }
    }

    private void disableButtons(){
        Button startTrackingButton = findViewById(R.id.start_button);
        startTrackingButton.setEnabled(false);
    }

    /**
     * Starts tracking (redirect to TrackingActivity) but only if if dep/job is set
     * @param view
     */
    public void startTracking(View view) {
        Log.d(TAG, "startTracking(): button selected, start tracking");
        if( ((department != null) && (job != null)) &&  (!(department.isEmpty()) && !(job.isEmpty())) ){
            Log.d(TAG, "Department is: '"+department+ "' and Job is: '"+job+ "'");

            Intent intent = new Intent(this, TrackingActivity.class);
            intent.putExtra(DEPARTMENT, department);
            intent.putExtra(JOB, job);
            startActivity(intent);
        }
    }

    /**
     * <p>Callback method to be invoked when an item in this view has been
     * selected. This callback is invoked only when the newly selected
     * position is different from the previously selected position or if
     * there was no selected item.</p>
     * <p>
     * Implementers can call getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param parent   The AdapterView where the selection happened
     * @param view     The view within the AdapterView that was clicked
     * @param position The position of the view in the adapter
     * @param id       The row id of the item that is selected
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.v(TAG, "onItemSelected(): enter");

        switch (parent.getId()){
            case R.id.spinner_department:
                Log.d(TAG, "onItemSelected(): department changed");
                department = parent.getItemAtPosition(position).toString();
                break;
            case R.id.spinner_job:
                Log.d(TAG, "onItemSelected(): job changed");
                job = parent.getItemAtPosition(position).toString();
                break;
            default:
                Log.d(TAG, "onItemSelected(): item selected, but no values changed?");
        }

        Log.v(TAG, "onItemSelected(): leave");
    }

    /**
     * Callback method to be invoked when the selection disappears from this
     * view. The selection can disappear for instance when touch is activated
     * or when the adapter becomes empty.
     *
     * @param parent The AdapterView that now contains no selected item.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
