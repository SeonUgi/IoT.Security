package com.example.iotsecurity;

import android.bluetooth.BluetoothClass;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;
import java.util.zip.DataFormatException;

public class SearchActivity<i> extends AppCompatActivity {
    RecyclerView recyclerView = null;
    SearchAdapter adapter;

    DatabaseReference mDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search2);


        String productNum = getIntent().getExtras().get("productNum").toString();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Products").child(productNum);

        /**
         * 리사이클러 뷰 생성
         */
        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemViewCacheSize(0);
        adapter = new SearchAdapter();
        adapter.clearItems();

        adapter.setOnItemClickListener(new ProductAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {

                /**
                 * 채워 넣을 값 선택하면 넣어짐
                 * 어떤식으로 선택할지
                 * 월요일에 논의 후 적용
                 */
                Product dataForDB = (Product)getIntent().getExtras().get("product");;
                Product beforeDB = (Product)adapter.getItem(position);

                dataForDB.always = beforeDB.always;

                dataForDB.infoType = beforeDB.infoType;
                dataForDB.deviceType = beforeDB.deviceType;
                dataForDB.serviceType = beforeDB.serviceType;
                dataForDB.resourceType = beforeDB.resourceType;
                dataForDB.portable = beforeDB.portable;
                dataForDB.piId = beforeDB.piId;

                /**
                 * GET Time Test
                 * DB에는 연결된 시각을 저장
                 * 불러오거나 넣을때 마다 period +1
                 * cycle은 연결해제(삭제) 할때까지 유지
                 * detail fragment 에서 데이터 출력할 때에는 연결 기간으로 (현재시각 - 연결시각)
                 * 제품에 대해 등록했을 때의 시간을 String으로 저장
                 */
                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/HH:mm", java.util.Locale.getDefault());
                dataForDB.cycle = dateFormat.format(date);

                dataForDB.period = 1;

                dataForDB.score = getRiskScore(dataForDB);

                mDatabase.setValue(dataForDB);

                adapter.clearItems();
                // Back to Product List(Product Fragment)
                finish();

            }
        });
        // 현재 저장되어 있는 제품의 data
        Product product = (Product)getIntent().getExtras().get("product");
        Vector<Double> productVector = getProductVector(product);

        TextView tv = findViewById(R.id.tv);

        ArrayList<Resource> resourceMain;
        ArrayList<DeviceInfo2> deviceMain;
        try {
            resourceMain = readResourceInfo();
            deviceMain = readDeviceInfo();

            ArrayList<DeviceInfo2> sortedDeviceMain = CalculateCos(productVector, deviceMain);
            Collections.sort(sortedDeviceMain);

            ArrayList<Product> similarProducts = top5Print(sortedDeviceMain, resourceMain);
            /**
             * GET Time Test
             * DB에는 연결된 시각을 저장
             * 불러오거나 넣을때 마다 period +1
             * cycle은 연결해제(삭제) 할때까지 유지
             * detail fragment 에서 데이터 출력할 때에는 연결 기간으로 (현재시각 - 연결시각)
             */
            long now = System.currentTimeMillis();
            Date date = new Date(now);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/HH:mm", java.util.Locale.getDefault());
            Date testDate = dateFormat.parse("2020/11/01/00:00");
            long diff = (date.getTime() - testDate.getTime())/1000;
            date = new Date(diff);

//            tv.setText(dateFormat.format(date));

            adapter.setItems(similarProducts);
            adapter.notifyDataSetChanged();
            recyclerView.setAdapter(adapter);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

    }

    /**
     * 제품 데이터를 받아서 리사이클러 뷰로 출력
     * 리사이클러뷰에는 클릭 리스너
     * 아이템 클릭시 해당 리소스를 현재 Product내용과 병합 (병합할 때 제품 정보에 비어있는 값만 채워 넣음)
     * 병합 후에는 DB에 저장
     * 이 저장된 Product 데이터는 ProductFragment에서 출력.
     */

    private Vector<Double> getProductVector(Product product) {
        Vector<Double> productVec = new Vector<Double>(13);
        if(product.provider.equals("Philips"))
            productVec.addElement((Double)1.0);
        else
            productVec.addElement((Double)0.0);

        if(product.category.equals("전구"))
            productVec.addElement((Double)1.0);
        else
            productVec.addElement((Double)0.0);

        if(product.category.equals("조명"))
            productVec.addElement((Double)1.0);
        else
            productVec.addElement((Double)0.0);

        // 장치 이동성 true:이동형, fasle:고정형
        if(product.portable) {
            productVec.addElement((Double)0.0);
            productVec.addElement((Double)1.0);
        }
        else {
            productVec.addElement((Double)1.0);
            productVec.addElement((Double)0.0);
        }


        //연결 방식 wifi, bluetooth, z-wave 순서
        if(product.connection.contains("wifi"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);
        if(product.connection.contains("bluetooth"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);
        if(product.connection.contains("z-wave"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);

        //디바이스 타입 : 스마트장비, 센서, 액츄에이터 순서
        if(product.deviceType.contains("스마트장비"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);
        if(product.deviceType.contains("센서"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);
        if(product.deviceType.contains("액츄에이터"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);

        // 디스플레이 유무 : 있음, 없음 순서
        if(product.display) {
            productVec.addElement((Double) 1.0);
            productVec.addElement((Double) 0.0);
        }
        else {
            productVec.addElement((Double) 0.0);
            productVec.addElement((Double) 1.0);
        }

        if(product.category.equals("체중계"))
            productVec.addElement((Double)1.0);
        else
            productVec.addElement((Double)0.0);

        if(product.provider.equals("샤오미"))
            productVec.addElement((Double)1.0);
        else
            productVec.addElement((Double)0.0);
        return productVec;
    }

    //csv파일을 읽어서 devices에 정보 생성
    private ArrayList readResourceInfo() throws IOException {
        ArrayList<Resource> resources = new ArrayList<Resource>();
        InputStream is  = getResources().openRawResource(R.raw.resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("EUC-KR")));
        String line="";
        try {
            int idx = 0;
            while ((line = reader.readLine()) != null) {
                //Split by ','

                String[] tokens = line.split(",");
                Resource sample = new Resource();
                //제품명 얻기
                sample.setIdx(idx++);
                sample.setName(tokens[0]);
                sample.setCategory(tokens[1]);
                sample.setManufactureName(tokens[2]);
                sample.setMovement(tokens[3]);
                sample.setConnection(tokens[4]);
                sample.setGatheringMethod(tokens[5]);
                sample.setServices(tokens[6]);
                sample.setDataList(tokens[7]);
                sample.setDataType(tokens[8]);
                sample.setAgreement(Integer.parseInt(tokens[9]));
                sample.setDeviceType(tokens[10]);
                sample.setDisplay(Integer.parseInt(tokens[11]));
                sample.setRiskScore(Double.parseDouble(tokens[12]));
                resources.add(sample);
            }
        } catch (IOException e){
            Log.wtf("Search Activity", "error reading data file on line" + line,e);
        }

        return resources;
    }

    //csv파일을 읽어서 devices에 정보 생성
    private ArrayList<DeviceInfo2> readDeviceInfo() throws IOException {
        ArrayList<DeviceInfo2> devices = new ArrayList<DeviceInfo2>();
        InputStream is  = getResources().openRawResource(R.raw.hue);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("EUC-KR")));
        String line="";
        try {
            int idx = 0;
            while ((line = reader.readLine()) != null) {
                //Split by ','

                String[] tokens = line.split(",");
                DeviceInfo2 sample = new DeviceInfo2();
                //제품명 얻기
                sample.setIdx(idx++);
                sample.setName(tokens[0]);
                //데이터 구축
                //벡터 생성
                Vector<Double> vector = new Vector<Double>(13);
                vector.addElement(Double.parseDouble(tokens[1]));
                vector.addElement(Double.parseDouble(tokens[2]));
                vector.addElement(Double.parseDouble(tokens[3]));
                vector.addElement(Double.parseDouble(tokens[4]));
                vector.addElement(Double.parseDouble(tokens[5]));
                vector.addElement(Double.parseDouble(tokens[6]));
                vector.addElement(Double.parseDouble(tokens[7]));
                vector.addElement(Double.parseDouble(tokens[8]));
                vector.addElement(Double.parseDouble(tokens[9]));
                vector.addElement(Double.parseDouble(tokens[10]));
                vector.addElement(Double.parseDouble(tokens[11]));
                vector.addElement(Double.parseDouble(tokens[12]));
                vector.addElement(Double.parseDouble(tokens[13]));
                vector.addElement(Double.parseDouble(tokens[14]));
                vector.addElement(Double.parseDouble(tokens[15]));
                sample.setVector(vector);
                devices.add(sample);
            }
        } catch (IOException e){
            Log.wtf("Search Activity", "error reading data file on line" + line,e);
        }
        return devices;
    }
    //코사인 유사도 계산 함수 input 벡터(사용자가 선택한 속성들을 벡터로 변환한 값 v1과 v2 우리가 구축한 devices에 있는 벡터값 유사도 계산)
    private double getScore(Vector<Double> v1, Vector<Double> v2) throws Exception{
        int v1Size = v1.size();
        if (v1Size != v2.size()){
            throw new Exception("Vectors not same size");
        }
        double numerator = 0;
        double v1squaresum = 0;
        double v2squaresum = 0;
        for (int i = 0; i < v1Size; i++){
            double v1Val = v1.get(i);
            double v2Val = v2.get(i);
            numerator += (v1Val * v2Val);
            v1squaresum += (v1Val * v1Val);
            v2squaresum += (v2Val * v2Val);
        }
        if (numerator == 0 || v1squaresum == 0 || v2squaresum == 0){
            return 0;
        }
        double denom = (Math.sqrt(v1squaresum) * Math.sqrt(v2squaresum));
        return numerator / denom;
    }

    //사용자 입력 벡터와 조사 데이터의 유사도계산
    private ArrayList<DeviceInfo2> CalculateCos(Vector<Double> vec, ArrayList<DeviceInfo2> deviceList){
        for(DeviceInfo2 di : deviceList){
            double score = 0;
            try {
                score = getScore(vec, di.getVector());
            } catch (Exception e) {
                e.printStackTrace();
            }
            //DeviceInfo에 유사도 점수 입력
            di.setSimScore(score);
        }


        return deviceList;
    }
    //정렬된 리스트를 사용하여 top5 장치 출력
    private ArrayList<Product> top5Print(ArrayList<DeviceInfo2> sortedList, ArrayList<Resource> resources){
        //리스트에 뿌려주는 내용 필요
        ArrayList<Product> top5 = new ArrayList<Product>();
        for(int i=0; i<5; i++){
            Product temp = getIdxDPD(sortedList.get(i).getIdx(), resources);
            temp.score = Math.round((sortedList.get(i).getSimScore() * 100) * 100)/ 100.0;
            top5.add(temp);
            adapter.addItem(temp);
        }
        //리스트에서 해당 idx에 대한 클릭을 얻었을 때, 해당 productList의 해당 값의 idx를 얻어서 등록과정 종료
        //devices.get(productList.getIdx("사용자 선택 값"));



        return top5;
    }
    //만약 어떠한 값이 선택되었다고 하면 idx를 기반으로 원본 데이터 객체를 가져옴
    private Product getIdxDPD(int idx, ArrayList<Resource> reference){
        Resource r = reference.get(idx);
        Product p = new Product();
        r.getIdx();
        p.name = r.getName();
        p.category = r.getCategory();
        p.provider = r.getManufactureName();
        p.portable = r.getMovement().contains("이동형");
        p.connection = r.getConnection();
        if(r.getGatheringMethod().contains("상시수집"))
            p.always = 3;
        else if(r.getGatheringMethod().contains("조건수집"))
            p.always = 2;
        else
            p.always = 1;
        p.serviceType = r.getServices();
        p.infoType = r.getDataType() + "/" + r.getDataList();
        p.deviceType = r.getDeviceType();
        p.agree = (r.getAgreement()==1);
        p.display = (r.getDisplay()==1);
        p.score = r.getRiskScore();
        return p;
    }

    /**
     * 위험도 점수 계산
     *
     * @param product 계산할 제품 객체
     * @return 위험도 점수
     */
    private double getRiskScore(Product product) {

        double count = getCountScore(product.period, 15);
        double measure = getMeasureScore(product.always, 5);
        double move = getMoveScore(product.portable, 25);
        double connection = getConnectionScore(product.connection, 10);
        double service = getServiceScore(product.serviceType, 10);
        double dataType = getDataTypeScore(product.infoType, 15);
        double consent = getConsentScore(product.deviceType, product.display, 20);

        double result = count + measure + move + connection + service + dataType + consent;

        return Math.round(result * 100) / 100.0;
    }

    private double getServiceScore(String serviceType, double weight) {
        double service = 1;
        String[] biometricData = {"근육량", "bmi", "체중", "체지방", "내장지방", "기초대사", "바디점수", "수분",
                                "지방무게", "신체나이", "수분량", "기초대사량", "스트레스지수", "골피부", "골격량",
                                "지방외체중", "체형", "신체점수", "비만등급", "심박수", "혈압", "지문"};
        String[] mediaData = {"CCTV영상", "움직임감지", "이미지", "음성"};
        String[] gpsData = {"걸음수", "위치정보", "문열림감지", "출입감지"};
        String[] usingPatternData = {"전력량"};

        for(int i=0; i<usingPatternData.length; i++)
            if(serviceType.contains(usingPatternData[i])) {
                service = 2;
                break;
            }
        for(int i=0; i<gpsData.length; i++)
            if(serviceType.contains(gpsData[i])) {
                service = 3;
                break;
            }
        for(int i=0; i<mediaData.length; i++)
            if(serviceType.contains(mediaData[i])) {
                service = 4;
                break;
            }
        for(int i=0; i<biometricData.length; i++)
            if(serviceType.contains(biometricData[i])) {
                service = 5;
                break;
            }
        service = service / 5 * weight;

        return service;
    }

    private double getCountScore(long period, double weight) {
        return (double)period / 1000 * weight;

    }

    private double getMeasureScore(int always, double weight) {
        double measure = always;
        if(measure == 3)
            measure = 1;
        else if(measure == 2)
            measure = 0.5;
        else
            measure = 0;
        measure *= weight;

        return measure;
    }

    private double getMoveScore(boolean portable, double weight) {
        double move = 0.5;
        if(portable)
            move = 1;
        move *= weight;

        return move;
    }

    private double getConsentScore(String deviceType, boolean display, double weight) {
        double deviceTypeScore = 0;
        if(deviceType.contains("액츄에이터"))
            deviceTypeScore = 2;
        if(deviceType.contains("스마트장비"))
            deviceTypeScore = 3;
        double displayScore = 1;
        if(display)
            displayScore = 2;

        double consent = (deviceTypeScore + displayScore) / 5 * weight;

        return consent;
    }

    private double getConnectionScore(String connection, double weight) {
        double score = 0;
        if(connection.contains("z-wave"))
            score = 1;
        if(connection.contains("zigbee"))
            score = 2;
        if(connection.contains("bluetooth"))
            score = 3;
        if(connection.contains("wifi"))
            score = 4;
        score = score / 4 * weight;

        return score;
    }

    private double getDataTypeScore(String infoType, double weight) {
        double dataType = 0;
        if(infoType.contains("행태정보"))
            dataType = 1;
        if(infoType.contains("사용패턴"))
            dataType = 2;
        if(infoType.contains("생체정보") || infoType.contains("금융정보"))
            dataType = 3;
        dataType = dataType / 3 * weight;
        return dataType;
    }
}