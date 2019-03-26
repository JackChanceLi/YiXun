
package com.jackchance.yixun.ZAssets.QRCodeScan;

import android.content.Context;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;
import cn.easyar.Engine;

public class GLView extends GLSurfaceView
{
    private HelloAR helloAR;                //AR识别实例
    private HelloAR.MessageAlerter onAlert; //AR识别消息接口
    private QRScanResult qrScanResult;      //接口，返回二维码扫描信息

    //接口，将扫描结构传到activity
    public interface QRScanResult
    {
        void invoke(String result);
    }

    public GLView(final Context context)
    {
        super(context); //再context中创建view
        setEGLContextFactory(new ContextFactory());
        setEGLConfigChooser(new ConfigChooser());

        //helloAR实例
        helloAR = new HelloAR();

        //编写实现messageAlerter接口逻辑，从HelloAR获取信息
        onAlert = new HelloAR.MessageAlerter() {
            @Override
            public void invoke(final String s) {
                GLView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
                        qrScanResult.invoke(s);
                    }
                });
            }
        };


        //设置渲染器
        this.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                synchronized (helloAR) {
                    helloAR.initGL();
                }
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int w, int h) {
                synchronized (helloAR) {
                    helloAR.resizeGL(w, h);
                }
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                synchronized (helloAR) {
                    helloAR.render();
                }
            }
        });
        this.setZOrderMediaOverlay(true);
    }

    public void setQrScanResult(QRScanResult qrScanResult){
        this.qrScanResult = qrScanResult;
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        synchronized (helloAR) {
            if (helloAR.initialize(onAlert)) {
                helloAR.start();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow()
    {
        synchronized (helloAR) {
            helloAR.stop();
            helloAR.dispose();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Engine.onResume();
    }

    @Override
    public void onPause()
    {
        Engine.onPause();
        super.onPause();
    }

    private static class ContextFactory implements GLSurfaceView.EGLContextFactory
    {
        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig)
        {
            EGLContext context;
            int[] attrib = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib );
            return context;
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context)
        {
            egl.eglDestroyContext(display, context);
        }
    }

    private static class ConfigChooser implements GLSurfaceView.EGLConfigChooser
    {
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display)
        {
            final int EGL_OPENGL_ES2_BIT = 0x0004;
            final int[] attrib = { EGL10.EGL_RED_SIZE, 4, EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE };

            int[] num_config = new int[1];
            egl.eglChooseConfig(display, attrib, null, 0, num_config);

            int numConfigs = num_config[0];
            if (numConfigs <= 0)
                throw new IllegalArgumentException("fail to choose EGL configs");

            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, attrib, configs, numConfigs,
                    num_config);

            for (EGLConfig config : configs)
            {
                int[] val = new int[1];
                int r = 0, g = 0, b = 0, a = 0, d = 0;
                if (egl.eglGetConfigAttrib(display, config, EGL10.EGL_DEPTH_SIZE, val))
                    d = val[0];
                if (d < 16)
                    continue;

                if (egl.eglGetConfigAttrib(display, config, EGL10.EGL_RED_SIZE, val))
                    r = val[0];
                if (egl.eglGetConfigAttrib(display, config, EGL10.EGL_GREEN_SIZE, val))
                    g = val[0];
                if (egl.eglGetConfigAttrib(display, config, EGL10.EGL_BLUE_SIZE, val))
                    b = val[0];
                if (egl.eglGetConfigAttrib(display, config, EGL10.EGL_ALPHA_SIZE, val))
                    a = val[0];
                if (r == 8 && g == 8 && b == 8 && a == 0)
                    return config;
            }

            return configs[0];
        }
    }
}
