package com.payguard.fraud.grpc;

import com.payguard.fraud.dto.FraudScoreResult;
import com.payguard.fraud.service.FraudScoringOrchestrator;
import com.payguard.proto.fraud.FraudScoreRequest;
import com.payguard.proto.fraud.FraudScoreResponse;
import com.payguard.proto.fraud.FraudScoringServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class FraudGrpcService extends FraudScoringServiceGrpc.FraudScoringServiceImplBase {

    private final FraudScoringOrchestrator scoringOrchestrator;

    @Override
    public void scoreTransaction(FraudScoreRequest request,
                                  StreamObserver<FraudScoreResponse> responseObserver) {
        try {
            log.debug("gRPC scoring request for transaction: {}", request.getTransactionId());

            FraudScoreResult result = scoringOrchestrator.scoreTransaction(
                    request.getTransactionId(),
                    request.getMerchantId(),
                    request.getAmountCents(),
                    request.getCurrency(),
                    request.getCustomerEmail(),
                    request.getMerchantCategory(),
                    request.getCountry(),
                    request.getTimestamp()
            );

            FraudScoreResponse response = FraudScoreResponse.newBuilder()
                    .setTransactionId(result.getTransactionId())
                    .setFraudScore(result.getFraudScore())
                    .setDecision(result.getDecision())
                    .addAllContributingFactors(result.getContributingFactors())
                    .setModelVersion(result.getModelVersion())
                    .setLatencyMs(result.getLatencyMs())
                    .setFallbackUsed(result.isFallbackUsed())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC scoring failed for transaction {}: {}",
                    request.getTransactionId(), e.getMessage());
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Fraud scoring failed: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }
}