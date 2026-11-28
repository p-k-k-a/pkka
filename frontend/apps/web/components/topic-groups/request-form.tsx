"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  getListMyTopicGroupRequestsQueryKey,
  useSubmitTopicGroupRequest,
} from "@pkka/api";
import {
  TOPIC_GROUP_DESCRIPTION_MAX_LENGTH,
  TOPIC_GROUP_NAME_MAX_LENGTH,
  topicGroupRequestSubmitErrorMessage,
} from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

type RequestFormProps = {
  onSubmitted: () => void;
};

export function RequestForm({ onSubmitted }: RequestFormProps) {
  const queryClient = useQueryClient();
  const refreshMyRequests = () =>
    queryClient.invalidateQueries({ queryKey: getListMyTopicGroupRequestsQueryKey() });
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const { mutate: submitRequest, isPending } = useSubmitTopicGroupRequest<ApiError>({
    mutation: {
      onSuccess: async () => {
        await refreshMyRequests();
        onSubmitted();
      },
      onError: (error) => {
        setFormError(topicGroupRequestSubmitErrorMessage(error.status, error.body));
        // A 409 may mean a request is already open (e.g. sent from the phone): show it instead.
        if (error.status === 409) void refreshMyRequests();
      },
    },
  });

  const canSubmit = name.trim().length > 0 && description.trim().length > 0;

  return (
    <form
      className="space-y-6"
      onSubmit={(event) => {
        event.preventDefault();
        if (!canSubmit) return;
        setFormError(null);
        submitRequest({ data: { name: name.trim(), description: description.trim() } });
      }}
    >
      <Card className="bg-muted gap-6 rounded-2xl p-6 shadow-none ring-0 md:p-8">
        <div className="space-y-2">
          <Label htmlFor="topicGroupName" className="text-xs font-bold tracking-wider uppercase">
            Nazwa grupy<span className="text-destructive">*</span>
          </Label>
          <Input
            id="topicGroupName"
            required
            maxLength={TOPIC_GROUP_NAME_MAX_LENGTH}
            placeholder="np. Grupa AI"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </div>

        <div className="space-y-2">
          <Label
            htmlFor="topicGroupDescription"
            className="text-xs font-bold tracking-wider uppercase"
          >
            Opis<span className="text-destructive">*</span>
          </Label>
          <Textarea
            id="topicGroupDescription"
            required
            rows={6}
            maxLength={TOPIC_GROUP_DESCRIPTION_MAX_LENGTH}
            placeholder="Czym grupa będzie się zajmować i dla kogo jest?"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
          <p className="text-muted-foreground text-right text-xs font-medium tabular-nums">
            {description.length} / {TOPIC_GROUP_DESCRIPTION_MAX_LENGTH}
          </p>
        </div>
      </Card>

      {formError ? (
        <p role="alert" className="text-destructive text-center text-sm font-semibold">
          {formError}
        </p>
      ) : null}

      <Button
        type="submit"
        size="xl"
        className="w-full rounded-xl font-bold tracking-widest uppercase"
        disabled={!canSubmit || isPending}
      >
        {isPending ? "Wysyłanie…" : "Wyślij wniosek"}
      </Button>
    </form>
  );
}
