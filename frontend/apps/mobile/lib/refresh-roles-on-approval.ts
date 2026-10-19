import {
  ApplicationStatus,
  getGetMineQueryKey,
  getMeQueryKey,
  type getMineResponse,
} from "@pkka/api";
import { matchQuery, type QueryClient } from "@tanstack/react-query";

export function refreshRolesOnApproval(queryClient: QueryClient) {
  return queryClient.getQueryCache().subscribe((event) => {
    if (event.type !== "updated" || event.action.type !== "success") return;
    if (!matchQuery({ queryKey: getGetMineQueryKey(), exact: true }, event.query)) return;
    const response = event.action.data as getMineResponse | undefined;
    if (response?.status === 200 && response.data.status === ApplicationStatus.APPROVED) {
      void queryClient.invalidateQueries({ queryKey: getMeQueryKey() });
    }
  });
}
