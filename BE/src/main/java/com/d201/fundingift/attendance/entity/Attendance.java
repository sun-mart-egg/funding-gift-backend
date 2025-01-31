package com.d201.fundingift.attendance.entity;

import com.d201.fundingift._common.entity.BaseTime;
import com.d201.fundingift.attendance.dto.request.PostAttendanceRequest;
import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.funding.intrastructure.entity.FundingEntity;
import com.d201.fundingift.payment.entity.PaymentInfo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE attendance set deleted_at = DATE_ADD(NOW(), INTERVAL 9 HOUR) where attendance_id = ?")
public class Attendance extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id", nullable = false)
    private Long id;

    @Column(nullable = false, length = 20)
    private String sendMessageTitle;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String sendMessage;

    @Column(columnDefinition = "LONGTEXT", nullable = true)
    private String receiveMessage;

    @Column(nullable = false)
    private Integer price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_id", referencedColumnName = "consumer_id")
    private Consumer consumer; //펀딩에 참여한 소비자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id", referencedColumnName = "funding_id")
    private FundingEntity fundingEntity;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_info_id", referencedColumnName = "payment_info_id")
    private PaymentInfo paymentInfo;

    @Builder
    private Attendance(String sendMessageTitle, String sendMessage, String receiveMessage, Integer price, Consumer consumer, FundingEntity fundingEntity) {
        this.sendMessageTitle = sendMessageTitle;
        this.sendMessage = sendMessage;
        this.receiveMessage = receiveMessage;
        this.price = price;
        this.consumer = consumer;
        this.fundingEntity = fundingEntity;
    }

    public static Attendance from(PostAttendanceRequest postAttendanceRequest, Consumer consumer, FundingEntity fundingEntity) {
        return Attendance.builder()
                .sendMessageTitle(postAttendanceRequest.getSendMessageTitle())
                .sendMessage(postAttendanceRequest.getSendMessage())
                .price(postAttendanceRequest.getPrice())
                .consumer(consumer)
                .fundingEntity(fundingEntity)
                .build();
    }

    public void writingReceiveMessage(String msg) {
        this.receiveMessage = msg;
    }

    public void updatePaymentInfo(PaymentInfo paymentInfo) {
        this.paymentInfo = paymentInfo;
    }

}
