module com.angazo.lostrego.spy {
    requires com.angazo.lostrego;
    requires com.angazo.lostrego.spy.common;
    requires info.picocli;

    opens com.angazo.lostrego.spy to info.picocli;
}
