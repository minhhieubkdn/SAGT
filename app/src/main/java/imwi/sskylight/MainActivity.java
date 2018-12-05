package imwi.sskylight;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.format.Time;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    static String URI = "tcp://m15.cloudmqtt.com:19353";
    static String USER = "fngvccuw";
    static String PASSWORD = "snHS-dY1WY7S";
    final static String PUBLIC_TOPIC = "SAGT-Control";
    final static String SUBSCRIBE_TOPIC = "SAGT-Value";
    MqttAndroidClient client;

    final int NUM_OF_PLANTS = 3;

    private TextView tvConnectStt;
    private TextView tvDebug;
    private EditText[] etPlants = new EditText[NUM_OF_PLANTS];
    private EditText[] etTimeToWater = new EditText[NUM_OF_PLANTS];
    private EditText[] etTimeToMeasure = new EditText[NUM_OF_PLANTS];
    private EditText[] etWaterDuration = new EditText[NUM_OF_PLANTS];
    private Button btWater;
    private Button btReadHumidity;
    private Button btInitPlant;
    private Button btUpLoad;
    private Button btHome;
    private ImageButton btPlant[] = new ImageButton[NUM_OF_PLANTS];
    private TextView[] tvDisplayPlants = new TextView[NUM_OF_PLANTS];
    private TextView[] tvHumidity = new TextView[NUM_OF_PLANTS];
    private Spinner spFloorNum;
    private LinearLayout[] plantsLayout = new LinearLayout[NUM_OF_PLANTS];
    private EditText[] etMinHumidity = new EditText[NUM_OF_PLANTS];

    private Plants[] plants = new Plants[NUM_OF_PLANTS];
    private TreePositions[] treePos = new TreePositions[NUM_OF_PLANTS*2];
//    private Plants[] TreeInPots = new Plants[NUM_OF_PLANTS*2];

    Time currentTime = new Time();

    int color1 = Color.rgb(3,169,244);
    int color2 = Color.rgb(192,202,51);
    int color3 = Color.rgb(249,168,37);
    public int currentPos = 0;
    public int floorNum = 1;
    public boolean isInitializingPlants = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        allocateMemory();
        InitWidget();
        InitMQTTConnection();
        addActionFormWidgets();
        InitData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        InitDisplay();
        refreshNewFeed();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    void allocateMemory() {
        for(int i = 0; i < NUM_OF_PLANTS; i++) {
            plants[i] = new Plants();
        }
        for(int i = 0; i < NUM_OF_PLANTS*2; i++) {
            treePos[i] = new TreePositions();
            treePos[i].plant = new Plants();
        }
    }
    public void InitMQTTConnection() {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), URI, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        options.setUserName(USER);
        options.setPassword(PASSWORD.toCharArray());

        try {
            IMqttToken token = client.connect(options);

            tvConnectStt.setText("Connecting ...");

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    tvConnectStt.setText("Connection : Successful");
                    Subscribe(SUBSCRIBE_TOPIC);
                    Publish("Connected", "SAGT-Control");
                    Publish("SAGT Starting", "SAGT-Control");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    tvConnectStt.setText("Connection : Failed");

                }
            });
        } catch (MqttException e) {

            tvConnectStt.setText("Error");
            e.printStackTrace();
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                ProcessMQTTData(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void ProcessMQTTData(String topic, MqttMessage message) {
        String msg = message.toString();

        tvConnectStt.setText(topic);
        tvDebug.setText(msg);

        if (msg.charAt(0) == 'p') {
            int _currentPos;
            String _str[] = msg.split("-");

            _currentPos = Integer.parseInt(_str[1]);
            currentPos = _currentPos;
        }

        if (msg.charAt(0) == 'm') {
            String strArr[] = msg.split("-");
            int hum = Integer.parseInt(strArr[1]);
            treePos[currentPos].hum = hum;
            treePos[currentPos].plant.humidity = hum;
            refreshNewFeed();
            if(getCurrentFloor() == 1) {
                tvDebug.setText(Integer.toString(treePos[0].plant.getHumidity()) + "-" +
                        Integer.toString(treePos[1].plant.getHumidity()) + "-" +
                        Integer.toString(treePos[2].plant.getHumidity()) );
            } else {
                tvDebug.setText(Integer.toString(treePos[3].plant.getHumidity()) + "-" +
                        Integer.toString(treePos[4].plant.getHumidity()) + "-" +
                        Integer.toString(treePos[5].plant.getHumidity()) );
            }
        }

    }

    public void Subscribe(String topic) {
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void Publish(String msg, String topic) {
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = msg.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    public void InitWidget() {
        this.tvConnectStt = (TextView) findViewById(R.id.tv_stt_id);
        this.tvDebug = (TextView) findViewById(R.id.tv_debug_id);
        this.etPlants[0] = (EditText) findViewById(R.id.et_plant_name_id_1);
        this.etPlants[1] = (EditText) findViewById(R.id.et_plant_name_id_2);
        this.etPlants[2] = (EditText) findViewById(R.id.et_plant_name_id_3);
        this.etTimeToWater[0] = (EditText) findViewById(R.id.et_time_to_water_id_1);
        this.etTimeToWater[1] = (EditText) findViewById(R.id.et_time_to_water_id_2);
        this.etTimeToWater[2] = (EditText) findViewById(R.id.et_time_to_water_id_3);
        this.etTimeToMeasure[0] = (EditText) findViewById(R.id.et_time_for_next_water_1);
        this.etTimeToMeasure[1] = (EditText) findViewById(R.id.et_time_for_next_water_2);
        this.etTimeToMeasure[2] = (EditText) findViewById(R.id.et_time_for_next_water_3);
        this.etWaterDuration[0] = (EditText) findViewById(R.id.tv_time_for_water_1);
        this.etWaterDuration[1] = (EditText) findViewById(R.id.tv_time_for_water_2);
        this.etWaterDuration[2] = (EditText) findViewById(R.id.tv_time_for_water_3);
        this.btWater = (Button) findViewById(R.id.bt_water_id);
        this.btReadHumidity = (Button) findViewById(R.id.bt_read_humidity);
        this.btInitPlant = (Button) findViewById(R.id.bt_plant_id);
        this.btUpLoad = (Button) findViewById(R.id.bt_upload_id);
        this.btPlant[0] = (ImageButton) findViewById(R.id.ib_plant_1_id);
        this.btPlant[1] = (ImageButton) findViewById(R.id.ib_plant_2_id);
        this.btPlant[2] = (ImageButton) findViewById(R.id.ib_plant_3_id);
        this.tvDisplayPlants[0] = (TextView) findViewById(R.id._tv_plant_name_1_id);
        this.tvDisplayPlants[1] = (TextView) findViewById(R.id._tv_plant_name_2_id);
        this.tvDisplayPlants[2] = (TextView) findViewById(R.id._tv_plant_name_3_id);
        this.tvHumidity[0] = (TextView) findViewById(R.id.tv_hum_1_id);
        this.tvHumidity[1] = (TextView) findViewById(R.id.tv_hum_2_id);
        this.tvHumidity[2] = (TextView) findViewById(R.id.tv_hum_3_id);
        this.spFloorNum = (Spinner) findViewById(R.id.sp_floor_num_id);
        this.plantsLayout[0] = (LinearLayout) findViewById(R.id.ln_plant_1_id);
        this.plantsLayout[1] = (LinearLayout) findViewById(R.id.ln_plant_2_id);
        this.plantsLayout[2] = (LinearLayout) findViewById(R.id.ln_plant_3_id);
        this.btHome = (Button) findViewById(R.id.bt_home);
        this.etMinHumidity[0] = (EditText) findViewById(R.id.et_min_humidity_1);
        this.etMinHumidity[1] = (EditText) findViewById(R.id.et_min_humidity_2);
        this.etMinHumidity[2] = (EditText) findViewById(R.id.et_min_humidity_3);
    }

    public void InitData() {
        for (int i = 0; i < 3; i++) {
            switch (i) {
                case 0: plants[i].setName("Xà lách"); break;
                case 1: plants[i].setName("Rau cải"); break;
                case 2: plants[i].setName("Mùng tơi");break;
            }
            plants[i].setHour(7);
            plants[i].setMinute(30);
            plants[i].setTimeToMeasure(12);
            plants[i].setWaterDuration(5);
            plants[i].setHumidity(0);
            plants[i].id = i;
        }
        for (int i = 0; i < 6; i++) {
            treePos[i].setPosition(i);
            treePos[i].hum = 0;
            treePos[i].plant = plants[0];
        }
    }

    public void InitDisplay() {
        etPlants[0].setBackgroundColor(color1);
        etPlants[1].setBackgroundColor(color2);
        etPlants[2].setBackgroundColor(color3);
        btPlant[0].setBackgroundColor(color1);
        btPlant[1].setBackgroundColor(color2);
        btPlant[2].setBackgroundColor(color3);
        etPlants[0].setText(plants[0].getName());
        etTimeToWater[0].setText(Integer.toString(plants[0].hour) + ":" + Integer.toString(plants[0].minute));
        etTimeToMeasure[0].setText(Integer.toString(plants[0].getTimeToMeasure()));
        etWaterDuration[0].setText(Integer.toString(plants[0].getWaterDuration()));
        tvDisplayPlants[0].setText(plants[0].getName());
        tvHumidity[0].setText(Integer.toString(plants[0].getHumidity()));
        etPlants[1].setText(plants[1].getName());
        etTimeToWater[1].setText(Integer.toString(plants[1].hour) + ":" + Integer.toString(plants[1].minute));
        etTimeToMeasure[1].setText(Integer.toString(plants[1].getTimeToMeasure()));
        etWaterDuration[1].setText(Integer.toString(plants[1].getWaterDuration()));
        tvDisplayPlants[1].setText(plants[1].getName());
        tvHumidity[1].setText(Integer.toString(plants[1].getHumidity()));
        etPlants[2].setText(plants[2].getName());
        etTimeToWater[2].setText(Integer.toString(plants[2].hour) + ":" + Integer.toString(plants[2].minute));
        etTimeToMeasure[2].setText(Integer.toString(plants[2].getTimeToMeasure()));
        etWaterDuration[2].setText(Integer.toString(plants[2].getWaterDuration()));
        tvDisplayPlants[2].setText(plants[2].getName());
        tvHumidity[2].setText(Integer.toString(plants[2].getHumidity()));
    }

    public void addActionFormWidgets() {
        handleFloorNumSpinner();
        setOnClickButtonInitPlants();
        setOnClickButtonMeasureHumidity();
        setOnClickButtonWater();
        setOnClickButtonUpLoad();
        setOnClickButtonPlants();
        setEditTextsOnFocusChange();
        setOnClickDisplayLayoutPlants();
        btHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Publish("home",PUBLIC_TOPIC);
            }
        });
    }

    public void handleFloorNumSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.floorNumArray, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spFloorNum.setAdapter(adapter);
        spFloorNum.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    floorNum = 1;
                } else {
                    floorNum = 2;
                }
                refreshNewFeed();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                floorNum = 1;
            }
        });
    }

    public void setOnClickButtonInitPlants() {
        btInitPlant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isInitializingPlants = !isInitializingPlants;
                if (isInitializingPlants) {
                    for (int i = 0; i < 3; i++) {
                        plants[i].setName(etPlants[i].getText().toString());
                        plants[i].setTimeToWater(etTimeToWater[i].getText().toString());
                        plants[i].setWaterDuration(Integer.parseInt(etWaterDuration[i].getText().toString()));
                        plants[i].setTimeToMeasure(Integer.parseInt(etTimeToMeasure[i].getText().toString()));
                        plants[i].setMinHumidity(Integer.parseInt(etMinHumidity[i].getText().toString()));
                    }
                    tvDebug.setText("start init plants");
                    v.getBackground().setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                } else {
                    for ( int i = 0; i < NUM_OF_PLANTS; i++) {
                        plants[i].isChoosing = false;
                    }
                    tvDebug.setText("exit init plants");
                    v.getBackground().clearColorFilter();
                    v.invalidate();
                }
            }
        });
    }

    public void setOnClickButtonMeasureHumidity() {
        btReadHumidity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                measureHum();
            }
        });
    }

    public void setOnClickButtonWater() {
        btWater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                water();
            }
        });
    }

    public void setOnClickButtonUpLoad() {
        btUpLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String treeInfor;
                String time;
                String _garden = "g-";
                getAllPlantsInfor();
                for (int i = 0; i < 3; i++) {
                    treeInfor = "t" + Integer.toString(i + 1) + "-"
                            + plants[i].getTimeToWater() + ";"
                            + Integer.toString(plants[i].getTimeToMeasure()) + ";"
                            + Integer.toString(plants[i].getWaterDuration()) + ";"
                            + Integer.toString(plants[i].getMinHumidity());

                    Publish(treeInfor, PUBLIC_TOPIC);
                }
                for (int i = 0; i < 6; i++) {
                    _garden += treePos[i].plant.id;
                }
                Publish(_garden,PUBLIC_TOPIC);
                currentTime.setToNow();
                time = "time-" + Integer.toString(currentTime.hour) + ":" + Integer.toString(currentTime.minute);
                Publish(time, PUBLIC_TOPIC);
                Publish("update", PUBLIC_TOPIC);
            }
        });
    }

    public void getAllPlantsInfor() {
        for(int i = 0; i<3; i++) {
            plants[i].setName(etPlants[i].getText().toString());
            plants[i].setTimeToWater(etTimeToWater[i].getText().toString());
            plants[i].setTimeToMeasure(Integer.parseInt(etTimeToMeasure[i].getText().toString()));
            plants[i].setWaterDuration(Integer.parseInt(etWaterDuration[i].getText().toString()));
            plants[i].setMinHumidity(Integer.parseInt(etMinHumidity[i].getText().toString()));
        }
    }

    public void setOnClickButtonPlants() {
        btPlant[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInitializingPlants) {
                    plants[0].isChoosing = !plants[0].isChoosing;
                }

                if (plants[0].isChoosing) {
                    plants[1].isChoosing = false;
                    plants[2].isChoosing = false;
                    tvDebug.setText("choosing plant 1");
                } else {
                    tvDebug.setText("unchoosing plant 1");
                }
            }
        });

        btPlant[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInitializingPlants) {
                    plants[1].isChoosing = !plants[1].isChoosing;
                }
                if (plants[1].isChoosing) {
                    plants[0].isChoosing = false;
                    plants[2].isChoosing = false;
                    tvDebug.setText("choosing plant 2");
                } else {
                    tvDebug.setText("unchoosing plant 2");
                }
            }
        });

        btPlant[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInitializingPlants) {
                    plants[2].isChoosing = !plants[2].isChoosing;
                }
                if (plants[2].isChoosing) {
                    plants[1].isChoosing = false;
                    plants[0].isChoosing = false;
                    tvDebug.setText("choosing plant 3");
                } else {
                    tvDebug.setText("unchoosing plant 3");
                }
            }
        });
    }

    //everything need a loop at here
    public void setEditTextsOnFocusChange() {
        for (int i = 0; i < 3; i++) {
            etPlants[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideSoftKeyboard(v);
                    }
                }
            });
            etWaterDuration[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideSoftKeyboard(v);
                    }
                }
            });
            etTimeToWater[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (!b) {
                        hideSoftKeyboard(view);
                    }
                }
            });
            etTimeToMeasure[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (!b) {
                        hideSoftKeyboard(view);
                    }
                }
            });
            etMinHumidity[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (!b) {
                        hideSoftKeyboard(view);
                    }
                }
            });
        }
    }

    public void refreshNewFeed() {
        if (getCurrentFloor() == 1) {
            for (int i = 0; i < 3; i++) {
                tvDisplayPlants[i].setText(treePos[i].plant.getName());
                tvHumidity[i].setText(Integer.toString(treePos[i].hum));
                switch (treePos[i].plant.id) {
                    case 0: tvDisplayPlants[i].setTextColor(color1); break;
                    case 1: tvDisplayPlants[i].setTextColor(color2); break;
                    case 2: tvDisplayPlants[i].setTextColor(color3); break;
                }
            }
        } else {
            for (int i = 0; i < 3; i++) {
                tvDisplayPlants[i].setText(treePos[i+3].plant.getName());
                tvHumidity[i].setText(Integer.toString(treePos[i+3].hum));
                switch (treePos[i+3].plant.id) {
                    case 0: tvDisplayPlants[i].setTextColor(color1); break;
                    case 1: tvDisplayPlants[i].setTextColor(color2); break;
                    case 2: tvDisplayPlants[i].setTextColor(color3); break;
                }
            }
        }
    }

    public void setOnClickDisplayLayoutPlants() {
        plantsLayout[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInitializingPlants) {
                    for (int i = 0; i < 3; i++) {

                        if (plants[i].isChoosing) {

                            switch (i) {
                                case 0:
                                    tvDisplayPlants[0].setTextColor(color1);
                                    break;
                                case 1:
                                    tvDisplayPlants[0].setTextColor(color2);
                                    break;
                                case 2:
                                    tvDisplayPlants[0].setTextColor(color3);
                                    break;
                            }

                            if (getCurrentFloor() == 1) {
                                treePos[0].plant = plants[i].Clone();
                                tvDisplayPlants[0].setText(treePos[0].plant.getName());
                            } else {
                                treePos[3].plant = plants[i].Clone();
                                tvDisplayPlants[0].setText(treePos[3].plant.getName());
                            }
                        }
                    }
                } else {
                    if (getCurrentFloor() == 1) {
                        moveToPos(0);
                        tvDebug.setText("moving to " + Integer.toString(0));
                    } else {
                        moveToPos(3);
                        tvDebug.setText("moving to " + Integer.toString(3));
                    }
                }
            }
        });

        plantsLayout[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInitializingPlants) {
                    for (int i = 0; i < 3; i++) {
                        if (plants[i].isChoosing) {
                            switch (i) {
                                case 0:
                                    tvDisplayPlants[1].setTextColor(color1);
                                    break;
                                case 1:
                                    tvDisplayPlants[1].setTextColor(color2);
                                    break;
                                case 2:
                                    tvDisplayPlants[1].setTextColor(color3);
                                    break;
                            }

                            if (getCurrentFloor() == 1) {
                                treePos[1].plant = plants[i].Clone();
                                tvDisplayPlants[1].setText(treePos[1].plant.getName());
                            } else {
                                treePos[4].plant = plants[i].Clone();
                                tvDisplayPlants[1].setText(treePos[4].plant.getName());
                            }
                        }
                    }
                } else {
                    if (getCurrentFloor() == 1) {
                        moveToPos(1);
                        tvDebug.setText("moving to " + Integer.toString(1));
                    } else {
                        moveToPos(4);
                        tvDebug.setText("moving to " + Integer.toString(4));
                    }
                }
            }
        });

        plantsLayout[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInitializingPlants) {
                    for (int i = 0; i < 3; i++) {
                        if (plants[i].isChoosing) {
                            switch (i) {
                                case 0:
                                    tvDisplayPlants[2].setTextColor(color1);
                                    break;
                                case 1:
                                    tvDisplayPlants[2].setTextColor(color2);
                                    break;
                                case 2:
                                    tvDisplayPlants[2].setTextColor(color3);
                                    break;
                            }
                            if (getCurrentFloor() == 1) {
                                treePos[2].plant = plants[i].Clone();
                                tvDisplayPlants[2].setText(treePos[2].plant.getName());
                            } else {
                                treePos[5].plant = plants[i].Clone();
                                tvDisplayPlants[2].setText(treePos[5].plant.getName());
                            }
                        }
                    }
                } else {
                    if (getCurrentFloor() == 1) {
                        moveToPos(2);
                        tvDebug.setText("moving to " + Integer.toString(2));
                    } else {
                        moveToPos(5);
                        tvDebug.setText("moving to " + Integer.toString(5));
                    }
                }
            }
        });
    }

    protected void hideSoftKeyboard(View input) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    public void water() {
        Publish("water", PUBLIC_TOPIC);
    }

    public void measureHum() {
        Publish("measure", PUBLIC_TOPIC);
    }

    public void moveToPos(int _pos) {
        currentPos = _pos;
        String pos;
        String _move = "move";
        pos = "p-" + Integer.toString(_pos);
        Publish(pos,PUBLIC_TOPIC);
        Publish(_move, PUBLIC_TOPIC);
    }

    public int getCurrentFloor() {
        return this.floorNum;
    }
    public int getCurrentPos() {
        return this.currentPos;
    }
    public void equalsWith(Plants _plant1, Plants _plant2) {
        _plant1.name = _plant2.name;
        _plant1.hour = _plant2.hour;
        _plant1.minute = _plant2.minute;
        _plant1.intervalToMeasure = _plant2.intervalToMeasure;
        _plant1.waterDuration = _plant2.waterDuration;
        _plant1.humidity = _plant2.humidity;
        _plant1.id = _plant2.id;
        _plant1.minHumidity = _plant2.minHumidity;
    }
}
