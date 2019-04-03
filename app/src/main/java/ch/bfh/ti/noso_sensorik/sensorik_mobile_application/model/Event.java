package ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model;

import java.time.LocalDate;
import java.time.LocalTime;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;

@Data
@Entity
public class Event {
    private @Id @GeneratedValue Long id;
    private LocalDate date;
    private LocalTime time;
    private String station;
    private String job;
    private EventTrigger trigger;

    @ManyToOne(cascade = {CascadeType.ALL})
    private Beacon eventSource;
//	private DataCollectionNode collectedFrom;

    public Event(LocalDate date, LocalTime time, String station, String job, EventTrigger trigger, Beacon eventSource) {
        this.date = date;
        this.time = time;
        this.station = station;
        this.job = job;
        this.trigger = trigger;
        this.eventSource = eventSource;
    }
}