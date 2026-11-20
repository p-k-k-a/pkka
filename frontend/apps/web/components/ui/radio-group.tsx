"use client";

import * as React from "react";
import { RadioGroup as RadioGroupPrimitive } from "radix-ui";

import { cn } from "@/lib/utils";

function RadioGroup({
  className,
  ...props
}: React.ComponentProps<typeof RadioGroupPrimitive.Root>) {
  return (
    <RadioGroupPrimitive.Root
      data-slot="radio-group"
      className={cn("grid w-full gap-2", className)}
      {...props}
    />
  );
}

function RadioGroupItem({
  className,
  ...props
}: React.ComponentProps<typeof RadioGroupPrimitive.Item>) {
  return (
    // Same reason as in Checkbox: Radix renders a hidden, absolutely positioned input next
    // to the button, which needs a positioned parent inside the dashboard's scroll container.
    <span className="relative inline-flex shrink-0">
      <RadioGroupPrimitive.Item
        data-slot="radio-group-item"
        className={cn(
          "peer border-input focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:bg-input/30 dark:aria-invalid:border-destructive/50 dark:aria-invalid:ring-destructive/40 data-[state=checked]:border-primary data-[state=checked]:bg-primary dark:data-[state=checked]:bg-primary relative flex size-4 shrink-0 items-center justify-center rounded-full border transition-colors outline-none after:absolute after:-inset-x-3 after:-inset-y-2 focus-visible:ring-3 disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:ring-3",
          className,
        )}
        {...props}
      >
        <RadioGroupPrimitive.Indicator
          data-slot="radio-group-indicator"
          className="bg-primary-foreground size-1.5 rounded-full"
        />
      </RadioGroupPrimitive.Item>
    </span>
  );
}

export { RadioGroup, RadioGroupItem };
