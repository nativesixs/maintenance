package ocmaintenance;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class QueryHandler {

    public static <D extends Operation.Data, T, V extends Operation.Variables> CompletableFuture<T> execute(Query<D, T, V> query, ApolloClient client) {
        CompletableFuture<T> future = new CompletableFuture<>();
        client.query(query).enqueue(new ApolloCall.Callback<T>() {
            @Override
            public void onResponse(@NotNull Response<T> response) {
                if (response.hasErrors()) {
                    String errors = Objects.requireNonNull(response.getErrors()).stream().map(Object::toString).collect(Collectors.joining(", "));
                    future.completeExceptionally(new ApolloException(errors));
                    return;
                }
                future.complete(response.getData());
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                future.completeExceptionally(e);
                System.out.println(e);
            }
        });

        return future;
    }



}
