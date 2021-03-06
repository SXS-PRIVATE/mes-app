package com.shu.messystem;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.zxing.client.android.CaptureActivity;
import com.shu.messystem.component.GetGeneralInfomation;
import com.shu.messystem.component.TipDialogForIos;
import com.shu.messystem.downtime.DownTimeFragementMain;
import com.shu.messystem.downtime_edit.DowntimeEditFragment;
import com.shu.messystem.outputperhour.OutputPerHourFragementMain;
import com.shu.messystem.plantime.PlanTimeForStopFragementMain;
import com.shu.messystem.result_bean.GetUserInfoBean;
import com.shu.messystem.update_component.UpdateServices;
import com.shu.messystem.update_component.UpdateVersion;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout = null;
    private FrameLayout includeCehuaLayout = null;
    public static Toolbar toolbar = null;
    private FragmentTransaction ft = null;
    private List<String> parent = null;
    private Map<String, List<String>> map = null;
    public Fragment currentFragment = null;//????????????Fragement ????????????
    final private String mainFlag = "MainFlag";//?????????????????????????????????Fragement
    private int refreshStat = 1;//??????????????????
    private Handler handler = null;
    private String grantString;//?????????????????????
    private String currentFragmentTag = "01";//?????????
    public static String gonghao = "null";//PlanTimeForStopFragment?????????
    public static String name = "null";//PlanTimeForStopFragment?????????
    private ServiceConnection conne;
    private ActionBarDrawerToggle drawerListener;
    private UpdateServices updateServices;
    private boolean isReplaceFragment=false;//????????????????????????????????????????????????fragment ????????????fragment

private int currGroupPosition;
private int currChildPosition;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        //??????currentFragment
        if (savedInstanceState != null) {
            restoreFragment(savedInstanceState.getString("TAG", currentFragmentTag));
        } else {//??????????????????
            //?????? ???fragment   supportV4?????????getSupportFragmentManager(),?????????getFragmentManager()
            showMainFragment();
        }

        //?????????????????????
        initCreateView();
        //????????? ?????????
        initActionBar();
        //????????? ????????????
        initCehuaLayout();
        //?????????????????????
        initConfig();
        //??????????????????
        queryGrantString();
        //drawerLayout.openDrawer(includeCehuaLayout);//???????????????????????????
        Log.e("MainActivity", "onCreate()");
        beginUpdateServ();
    }
    private void queryGrantString() {

                Call<GetUserInfoBean> request = ManageRetrofit.getRetroInter(MainActivity.this).getUserPermission(gonghao);
                request.enqueue(new Callback<GetUserInfoBean>() {
                    @Override
                    public void onResponse(Call<GetUserInfoBean> call, Response<GetUserInfoBean> response) {
                        if (response.body()!=null){
                            String msg = response.body().getMsg();
                            if ("success".equals(msg)) {//????????????
                                grantString = response.body().getPermission();

                            } else {
                                grantString="-1"+msg;
                            }
                        }else{
                            grantString="-1??????????????????????????????????????????";
                        }

                    }

                    @Override
                    public void onFailure(Call<GetUserInfoBean> call, Throwable t) {
                        grantString="-1??????????????????????????????????????????";
                    }
                });

    }

    private void initConfig() {
        SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.Login_Info_SHARED, MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPreferences.edit();//?????? ?????????
        edit.putString("server_addr", sharedPreferences.getString("server_addr", SettingActivity.serAddr));
        edit.putString("update_addr", sharedPreferences.getString("update_addr", SettingActivity.updaAddr));
        edit.putString("updateinfo_filename", sharedPreferences.getString("updateinfo_filename", SettingActivity.updaFileName));

        edit.apply();
    }

    private void beginUpdateServ() {
        conne = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                UpdateServices.MyBinder myBinder = (UpdateServices.MyBinder) service;
                myBinder.setContext(MainActivity.this, handler);
                updateServices=myBinder.getService();
                Log.e("setContext ac", "setContext ac");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                updateServices=null;
            }
        };
        Intent serv = new Intent(MainActivity.this, UpdateServices.class);
        bindService(serv, conne, Service.BIND_AUTO_CREATE);
    }

    private void restoreFragment(String restoreTag) {
        //????????????Fragment
        ft = getSupportFragmentManager().beginTransaction();
        Fragment frag;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 6; j++) {
                frag = getSupportFragmentManager().findFragmentByTag(String.valueOf(i) + String.valueOf(j));
                if (frag != null) {
                    ft.hide(frag);
                }
            }
        }
        ft.commit();

        //?????? ??????Activity??????????????? ?????????Fragment

        frag = getSupportFragmentManager().findFragmentByTag(restoreTag);
        if (frag == null) {
            showMainFragment();
        } else {
            currentFragment = frag;
            ft = getSupportFragmentManager().beginTransaction();
            ft.show(frag)
                    .commit();
        }

    }

    private void initCreateView() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);//????????? DrawerLayout??????
        includeCehuaLayout = (FrameLayout) findViewById(R.id.include_cehua_layout);//????????????
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        parent = new ArrayList<>();
        map = new HashMap<>();
        handler = new Handler();

    }

    private void initActionBar() {
        toolbar.setTitle("MES??????");
        toolbar.setSubtitle("????????????");
        //toolbar.setTitleTextColor(Color.parseColor("#ffffff"));
        //toolbar.setSubtitleTextColor(Color.parseColor("#ffffff"));
        setSupportActionBar(toolbar);
        ActionBar a = getSupportActionBar();
        if (a != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        //????????????
        /*ActionBarDrawerToggle menuAction=new ActionBarDrawerToggle(LoginedMain.this,drawerLayout,toolbar, 0, 0) {
                          @Override
				          public void onDrawerOpened(View drawerView) {
				              super.onDrawerOpened(drawerView);
				               // mAnimationDrawable.stop();
				            }
				           @Override
				           public void onDrawerClosed(View drawerView) {
				                super.onDrawerClosed(drawerView);
				               // mAnimationDrawable.start();
				            }
			 };*/
        //????????????????????????
        toolbar.setNavigationOnClickListener((View v) -> {
                    drawerLayout.openDrawer(includeCehuaLayout);//??????????????????
                }
        );


    }

    private void initCehuaLayout() {
        //????????????
        TextView userInfo = ((TextView) findViewById(R.id.cehua_userid));
        gonghao = getIntent().getStringExtra("userId");
        if (userInfo != null) {
            userInfo.setText(gonghao);
        }
        //????????????
        name = this.getIntent().getStringExtra("userName");
        userInfo = ((TextView) findViewById(R.id.cehua_username));
        if (userInfo != null) {
            userInfo.setText(name);
        }

        ExpandableListView cehuaLayoutListView = (ExpandableListView) findViewById(R.id.expandablelistview1);
        if (cehuaLayoutListView != null) {
            cehuaLayoutListView.setAdapter(new ExpandAdapter());//????????????

            cehuaLayoutListView.setOnChildClickListener((ExpandableListView parent, View v, int groupPosition, int childPosition,
                                                         long id) -> {
                        //?????? ????????? ????????????????????????
                        //?????????groupPosition=  0 ,???????????????childPosition =   0
                        cehuaOnItemClick(groupPosition, childPosition);
                        return true;
                    }
            );
            //????????????????????????
            cehuaLayoutListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent, View childView, int flatPos, long id) {
                    if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                        long packedPos = ((ExpandableListView) parent).getExpandableListPosition(flatPos);
                        int groupPosition = ExpandableListView.getPackedPositionGroup(packedPos);
                        int childPosition = ExpandableListView.getPackedPositionChild(packedPos);

                        if(!isGrant(groupPosition,childPosition)){//??????
                            return false;
                        }
                        String mainStr= GetGeneralInfomation.md5(groupPosition + "" + childPosition);
                        if(TextUtils.isEmpty(mainStr)){
                            Toast.makeText(MainActivity.this, "?????????????????????.", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        //???????????????????????????????????????fragement
                        SharedPreferences sharedMainFrag = getSharedPreferences(mainFlag, MODE_PRIVATE);
                        SharedPreferences.Editor edit = sharedMainFrag.edit();
                        edit.putString(mainFlag, mainStr);
                        if (edit.commit()) {
                            Toast.makeText(MainActivity.this, "?????????????????????", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "?????????????????????", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                    return false;
                }

            });
        }
        drawerListener = new ActionBarDrawerToggle(this, drawerLayout,0,0) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                Log.e("??????????????????","??????????????????");
            }

            @Override//??????????????????????????? ??????fragment ???????????????
            public void onDrawerClosed(View drawerView) {
                //fragment?????????????????????
                String tag = String.valueOf(currGroupPosition) + String.valueOf(currChildPosition);//??????fragment??????
                Fragment temp = getSupportFragmentManager().findFragmentByTag(tag);

                Log.e("??????????????????","??????????????????");
                if(isReplaceFragment){//???????????????
                    if (temp != null) {
                        replaceFragment(null);
                    }else{
                        showFragment(currGroupPosition,currChildPosition);
                    }
                    isReplaceFragment=false;
                }
                super.onDrawerClosed(drawerView);

            }
        };
        drawerLayout.addDrawerListener(drawerListener);
    }

    private void showMainFragment() {//??????????????????????????????
        ft = getSupportFragmentManager().beginTransaction();
        SharedPreferences sharedMainFrag = this.getSharedPreferences(mainFlag, MODE_PRIVATE);
        currentFragmentTag = sharedMainFrag.getString(mainFlag, currentFragmentTag);
        if (currentFragmentTag.equals(GetGeneralInfomation.md5("01"))) {
        } else if (currentFragmentTag.equals(GetGeneralInfomation.md5("01"))) {
            currentFragment = new DownTimeFragementMain();

        } else if (currentFragmentTag.equals(GetGeneralInfomation.md5("02"))) {
            currentFragment = new DowntimeEditFragment();

        } else if (currentFragmentTag.equals(GetGeneralInfomation.md5("03"))) {
            currentFragment = new OutputPerHourFragementMain();

        } else if (currentFragmentTag.equals(GetGeneralInfomation.md5("16"))) {
            requestPower();//??????????????????

        } else if (currentFragmentTag.equals(GetGeneralInfomation.md5("20"))) {
            currentFragment = new PlanTimeForStopFragementMain();

        }
        if (currentFragment != null) {
            ft.add(R.id.fragement_frame, currentFragment, currentFragmentTag).commit();
        }
    }
    private boolean isGrant(int groupPosition, int childPosition){
        if(grantString==null){
            Toast.makeText(MainActivity.this, "???????????????????????????", Toast.LENGTH_SHORT).show();
 /*           if(!queryGrantThread.isAlive()){//????????????????????????died ???????????????????????????
                queryGrantString();
            }*/
            return false;
        }
        if (grantString.indexOf("-1")==0){
            Toast.makeText(MainActivity.this, grantString.substring(2), Toast.LENGTH_SHORT).show();
            queryGrantString();
            return  false;
        }
        if ( !grantString.contains(groupPosition + "" + childPosition)) {
            Toast.makeText(MainActivity.this, "??????????????????????????????", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    private void cehuaOnItemClick(int groupPosition, int childPosition) {
        if(!isGrant(groupPosition,childPosition)){//????????????
            return;
        }
        isReplaceFragment=true;
        currGroupPosition=groupPosition;
        currChildPosition=childPosition;
       drawerLayout.closeDrawer(includeCehuaLayout);//??????????????????

//??????titlebar?????????????????????
        String groupName = parent.get(groupPosition);//?????????
        String childName = map.get(groupName).get(childPosition);//????????????
        toolbar.setTitle(groupName);   //??????
        toolbar.setSubtitle(childName);//?????????

    }
    private void showFragment(int groupPosition,int childPosition){
        switch (groupPosition) {
            case 0://0 ???
                switch (childPosition) {
                    case 0://????????????
                        break;
                    case 1://???????????????
                        replaceFragment(new DownTimeFragementMain());
                        break;
                    case 2://???????????????
                        replaceFragment(new DowntimeEditFragment());
                        break;
                    case 3://???????????????
                        replaceFragment(new OutputPerHourFragementMain());
                        break;
                    default:
                }
                break;
            case 1://1???
                switch (childPosition) {
                    case 6://???????????????
                        requestPower();//??????????????????
                        break;
                    default:
                        Toast.makeText(MainActivity.this, "?????????", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2: //2??? ??????????????????
                switch (childPosition) {
                    case 0://??????????????????
                        replaceFragment(new PlanTimeForStopFragementMain());
                        break;
                    default:
                        Toast.makeText(MainActivity.this, "?????????", Toast.LENGTH_SHORT).show();
                }
                break;

        }
    }
    //????????????
    private void requestPower() {
        String grant[] = {Manifest.permission.CAMERA};

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                ) { //??????????????????????????????

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                    ) { //????????????????????????????????????????????????????????????????????????????????? true???
                Toast.makeText(this, "??????????????????????????????????????????????????????????????????????????????????????????", Toast.LENGTH_LONG).show();
            }

            ActivityCompat.requestPermissions(this, grant, 1);//????????????????????????????????????????????????????????????????????????1??????????????????????????????????????????onRequestPermissionsResult????????????????????????

        } else {
            startActivity(new Intent(this, CaptureActivity.class));
        }

    }

    //??????????????????
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { //??????????????????
                    startActivity(new Intent(this, CaptureActivity.class));
                } else { //??????????????????
                    Toast.makeText(this, "??????????????????????????????????????????", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    private void replaceFragment(Fragment fragement) {
        //????????????fragment???NULL???????????? ?????????fragment?????????

        String tag = String.valueOf(currGroupPosition) + String.valueOf(currChildPosition);//??????fragment??????
        if(fragement == null && currentFragment!=null ){//????????????????????????????????????
            Fragment temp=getSupportFragmentManager().findFragmentByTag(tag);
            Log.e("n",temp.getClass().getName());
            Log.e("n",currentFragment.getClass().getName());
            if(temp.getClass().getName().equals(currentFragment.getClass().getName())){//????????????????????????????????????????????????
                return;
            }

        }

        //????????????fragement
        ft = getSupportFragmentManager().beginTransaction();
      //  ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft .setCustomAnimations(R.anim.fragment_right_in_anim
                ,R.anim.fragment_left_out_anim
                ,R.anim.fragment_left_in_anim
                ,R.anim.fragment_right_out_anim);
        if (currentFragment != null) {
            ft.hide(currentFragment);
        }
        if (fragement != null) {
            ft.add(R.id.fragement_frame, fragement, tag);
            ft.show(fragement);
            currentFragment = fragement;
        } else {//????????????fragment???NULL???????????? ?????????fragment?????????
            currentFragment = getSupportFragmentManager().findFragmentByTag(tag);
            ft.show(currentFragment);
        }
                ft.commit();//?????????????????????
    }

    //??????  ????????? ????????????
    @SuppressWarnings("unchecked")
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.refresh) {
            if (refreshStat == 1) {
                refreshStat = 0;
                try {
                    @SuppressWarnings("rawtypes")

                    Class c = Class.forName(currentFragment.getClass().getName());//????????????
                    c.getDeclaredMethod("queryThread").invoke(currentFragment);//??????query????????????Fraement
                } catch (IllegalAccessException e) {
                    Log.e("MainActivity", "IllegalAccessException");
                } catch (InvocationTargetException e) {
                    Log.e("MainActivity", "InvocationTargetException");
                } catch (NoSuchMethodException e) {
                    Log.e("MainActivity", "NoSuchMethodException");
                } catch (ClassNotFoundException e) {
                    Log.e("MainActivity", "ClassNotFoundException");
                }
                //???????????????????????????
                new Handler().postDelayed(() -> {
                    refreshStat = 1;
                }, 1000 * 10); //5????????????????????????

            } else {
                Toast.makeText(MainActivity.this, "???????????????,???10????????????", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.exit) {//?????????????????????
            startActivity(new Intent(MainActivity.this, com.shu.messystem.main_login.LoginActivity.class));
            //???????????????
            SharedPreferences sharedLogin = this.getSharedPreferences(LoginActivity.Login_Info_SHARED, MODE_PRIVATE);
            SharedPreferences.Editor edit = sharedLogin.edit();
            edit.remove("username");
            edit.apply();
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("InflateParams")
    class ExpandAdapter extends BaseExpandableListAdapter implements View.OnTouchListener {
        Drawable[][] drawable = new Drawable[3][];
        float mPosX;//???????????????
        float mPosY;
        float mCurPosX;//???????????????
        float mCurPosY;

        ExpandAdapter() {
            initDrawable();

            initList();

        }

        @SuppressWarnings("deprecation")
        private void initDrawable() {
            //????????? ??????????????????
            if (VERSION.SDK_INT <= 17) {
                drawable[0] = new Drawable[4];
                drawable[0][0] = getResources().getDrawable(R.drawable.search);
                drawable[0][1] = getResources().getDrawable(R.drawable.search);
                drawable[0][2] = getResources().getDrawable(R.drawable.search);
                drawable[0][3] = getResources().getDrawable(R.drawable.search);

                drawable[1] = new Drawable[7];
                drawable[1][0] = getResources().getDrawable(R.drawable.case3d2);
                drawable[1][1] = getResources().getDrawable(R.drawable.anjianyi);
                drawable[1][2] = getResources().getDrawable(R.drawable.book);
                drawable[1][3] = getResources().getDrawable(R.drawable.nengxiaotie);
                drawable[1][4] = getResources().getDrawable(R.drawable.engineererror);
                drawable[1][5] = getResources().getDrawable(R.drawable.washing);
                drawable[1][6] = getResources().getDrawable(R.drawable.search);

                drawable[2] = new Drawable[3];
                drawable[2][0] = getResources().getDrawable(R.drawable.config);
                drawable[2][1] = getResources().getDrawable(R.drawable.user);
                drawable[2][2] = getResources().getDrawable(R.drawable.config);
            } else {//android 4.0??????????????????
                drawable[0] = new Drawable[4];
                drawable[0][0] = getResources().getDrawable(R.drawable.search, null);
                drawable[0][1] = getResources().getDrawable(R.drawable.search, null);
                drawable[0][2] = getResources().getDrawable(R.drawable.search, null);
                drawable[0][3] = getResources().getDrawable(R.drawable.search);

                drawable[1] = new Drawable[7];
                drawable[1][0] = getResources().getDrawable(R.drawable.case3d2, null);
                drawable[1][1] = getResources().getDrawable(R.drawable.anjianyi, null);
                drawable[1][2] = getResources().getDrawable(R.drawable.book, null);
                drawable[1][3] = getResources().getDrawable(R.drawable.nengxiaotie, null);
                drawable[1][4] = getResources().getDrawable(R.drawable.engineererror, null);
                drawable[1][5] = getResources().getDrawable(R.drawable.washing, null);
                drawable[1][6] = getResources().getDrawable(R.drawable.qrcode_scan, null);

                drawable[2] = new Drawable[3];
                drawable[2][0] = getResources().getDrawable(R.drawable.clock_plan, null);
                drawable[2][1] = getResources().getDrawable(R.drawable.user, null);
                drawable[2][2] = getResources().getDrawable(R.drawable.config, null);
            }
        }

        private void initList() {
            parent.add("??????????????????");
            parent.add("??????????????????");
            parent.add("??????????????????");

            List<String> productionList = new ArrayList<>();
            productionList.add("??????????????????");
            productionList.add("?????????????????????");
            productionList.add("A08IP/MAC??????");
            productionList.add("?????????????????????");
            map.put("??????????????????", productionList);

            List<String> qualityList = new ArrayList<>();
            qualityList.add("?????????????????????");
            qualityList.add("?????????????????????");
            qualityList.add("?????????????????????");
            qualityList.add("?????????????????????");
            qualityList.add("??????????????????");
            qualityList.add("??????????????????");
            qualityList.add("?????????????????????");
            map.put("??????????????????", qualityList);

            List<String> configList = new ArrayList<>();
            configList.add("??????????????????");
            configList.add("??????????????????");
            configList.add("??????????????????");
            /*
			configList.add("child3-2");
			configList.add("child3-3");
			*/
            map.put("??????????????????", configList);
        }

        //?????? ??? ??????
        public int getGroupCount() {
            // TODO Auto-generated method stub
            return parent.size();
        }

        //???????????????item?????????item?????????
        public int getChildrenCount(int groupPosition) {
            String key = parent.get(groupPosition);//???
            return map.get(key).size();//?????????
        }

        //???????????????item?????????
        public Object getGroup(int groupPosition) {
            return parent.get(groupPosition);
        }

        //?????????item?????????????????????
        public Object getChild(int groupPosition, int childPosition) {
            String key = parent.get(groupPosition);//?????????
            return (map.get(key).get(childPosition));
        }

        @Override
        public long getGroupId(int groupPosition) {
            // TODO Auto-generated method stub
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            // TODO Auto-generated method stub
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            // TODO Auto-generated method stub
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.expandable_parent, null);

            }
            TextView tv = (TextView) convertView.findViewById(R.id.parent_textview);
            tv.setText(MainActivity.this.parent.get(groupPosition));
            return convertView;
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                                 ViewGroup parent) {
            {
                String key = MainActivity.this.parent.get(groupPosition);
                String info = map.get(key).get(childPosition);
                ViewHolder viewHolder=null;
                if (convertView == null) {
                    viewHolder=new ViewHolder();
                        convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.expandable_children, null);
                        convertView.setTag(viewHolder);
                }else{
                    viewHolder=(ViewHolder)convertView.getTag();
                }
                viewHolder. tv = (TextView) convertView.findViewById(R.id.children_textview);
                viewHolder. tv.setText(info);

                //?????????????????????
                Drawable drawable_temp = drawable[groupPosition][childPosition];
                //Drawable drawable= getResources().getDrawableForDensity(R.drawable.find, DisplayMetrics.DENSITY_XHIGH, null);
                drawable_temp.setBounds(drawable_temp.getMinimumWidth(), 0, drawable_temp.getMinimumWidth() * 2, drawable_temp.getMinimumHeight());
                viewHolder. tv.setCompoundDrawables(drawable_temp, null, null, null);//??????TextView???drawableleft
                viewHolder. tv.setCompoundDrawablePadding(10);//???????????????text???????????????
                //tv.setOnTouchListener(this);//???????????????????????????
                // tv.setOnLongClickListener(this);

                return convertView;
            }
        }
        class ViewHolder{
            TextView tv;

        }
        //????????????
        public boolean onTouch(View v, MotionEvent event) {
            Log.e("POSX", String.valueOf(event.getX()));
            Log.e("POSY", String.valueOf(event.getY()));
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mPosX = event.getX();
                    mPosY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mCurPosX = event.getX();
                    mCurPosY = event.getY();

                    break;
                case MotionEvent.ACTION_UP:
                    Log.e("CHA", String.valueOf(mCurPosY - mPosY));
                    Log.e("CHA", String.valueOf(mCurPosX - mPosX));
                    if (mCurPosY - mPosY > 0) {
                        //????????????
                        Log.e("????????????", "????????????");

                    }
                    if (mCurPosY - mPosY < 0) {
                        //????????????
                        Log.e("????????????", "????????????");
                    }
                    if (mCurPosX - mPosX < 0) {
                        //????????????
                        Log.e("????????????", "????????????");
                    }
                    if (mCurPosX - mPosX > 0) {
                        //????????????
                        Log.e("????????????", "????????????");
                    }

                    break;
            }
            return true;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            // TODO Auto-generated method stub
            return true;
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        //??????????????????  
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public void onStart() {
        super.onStart();
        Log.e("MainActivity", "onStart()");
    }

    public void onResume() {
        super.onResume();
        if (currentFragment == null) {//???????????????????????????????????????
            currGroupPosition=0;
            currChildPosition=1;
            replaceFragment(new DownTimeFragementMain());
        }
        Log.e("MainActivity", "onResume()");
    }

    public void onPause() {
        super.onPause();
        Log.e("MainActivity", "onPause()");
    }

    public void onStop() {
        super.onStop();
        Log.e("MainActivity", "onStop()");
    }

    public void onDestroy() {
        super.onDestroy();
        updateServices.onDestroyActivity();
        try {
            unbindService(conne);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        Log.e("MainActivity", "onDestory()");
    }

    protected void onSaveInstanceState(Bundle outState) {
        Log.e("MainActivity", "onSaveInstanceState()");

        //????????????Activity????????? Fragment??? ??????   ???????????????  ???????????????  onCreate????????????????????????Fragment
        if (currentFragment != null) {
            outState.putString("TAG", currentFragment.getTag());
        }

        super.onSaveInstanceState(outState);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode== UpdateVersion.SetInstallApkPermi && updateServices!=null){
            //android8.0????????????apk??? ????????????
            updateServices.setInstallApkPermiResult();
        }

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                tipExit();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    //????????????
    private void tipExit(){
        TipDialogForIos.builder builder=new TipDialogForIos.builder(this);
        final TipDialogForIos tipDialogForIos=builder.setMsg("????????????????")
                .setTitle("??????")
                .setConfirmText("??????")
                .create();
        tipDialogForIos.setConfirmListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//???????????????
                tipDialogForIos.getAlertDialog().dismiss();
                finish();
            }
        });
        tipDialogForIos.setCancelListener(new View.OnClickListener() {//?????????????????????
            @Override
            public void onClick(View v) {
                tipDialogForIos.getAlertDialog().dismiss();
            }
        });
        tipDialogForIos.showTip();
    }
}
