import { Recall } from '../api/generated';

/** The most recent reminder of a document. The API lists them newest first; do not rely on the order. */
export function latestRecall(recalls: Recall[] | undefined): Recall | undefined {
  return [...(recalls ?? [])].sort((a, b) => (b.degree ?? 0) - (a.degree ?? 0) || (b.id ?? 0) - (a.id ?? 0))[0];
}
