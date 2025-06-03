package com.aquatrack.common.mqtt;

import com.aquatrack.common.websocket.SensorSocketSender;
import com.aquatrack.sensor.dto.WaterQualityLogRequest;
import com.aquatrack.sensor.entity.WaterQualityLog;
import com.aquatrack.sensor.service.WaterQualityLogService;
import com.aquatrack.aquarium.entity.Aquarium;
import com.aquatrack.aquarium.repository.AquariumRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttSensorSubscriber {

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;
    private final WaterQualityLogService logService;
    private final AquariumRepository aquariumRepository;
    private final SensorSocketSender sensorSocketSender;

    @PostConstruct
    public void subscribeToSensorData() {
        try {
            mqttClient.subscribe("aquatrack/sensor", (topic, message) -> {
                String payload = new String(message.getPayload());
                log.info("📡 센서 MQTT 수신됨: topic={}, payload={}", topic, payload);

                try {
                    // 1. JSON payload 파싱
                    JsonNode json = objectMapper.readTree(payload);

                    String boardId = json.get("boardId").asText();
                    float temperature = (float) json.get("temperature").asDouble();
                    float ph = (float) json.get("ph").asDouble();
                    float turbidity = (float) json.get("turbidity").asDouble();

                    // 2. boardId로 어항 조회
                    Aquarium aquarium = aquariumRepository.findByBoardId(boardId)
                            .orElseThrow(() -> new RuntimeException("등록되지 않은 보드 ID입니다: " + boardId));

                    // 3. DTO 생성 후 저장
                    WaterQualityLogRequest request = new WaterQualityLogRequest();
                    request.setUserId(String.valueOf(aquarium.getUser().getUserId()));
                    request.setAquariumId(aquarium.getAquariumId());
                    request.setTemperature(temperature);
                    request.setPH(ph);
                    request.setTurbidity(turbidity);

                    WaterQualityLog savedLog = logService.save(request);
                    sensorSocketSender.send(savedLog); // ✅ WebSocket 알림도 유지

                } catch (Exception e) {
                    log.error("❌ 센서 메시지 처리 실패: {}", e.getMessage(), e);
                }
            });

            log.info("✅ MQTT 센서 토픽 구독 성공");

        } catch (MqttException e) {
            log.error("❌ MQTT 센서 토픽 구독 실패: {}", e.getMessage(), e);
        }
    }
}
